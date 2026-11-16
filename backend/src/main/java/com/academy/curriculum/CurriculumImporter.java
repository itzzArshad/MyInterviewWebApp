package com.academy.curriculum;

import com.academy.lesson.LessonKind;
import com.academy.lesson.LessonParser;
import com.academy.lesson.LessonParser.ParsedLesson;
import com.academy.lesson.LessonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads the curriculum map from {@code classpath:curriculum/curriculum.yml} at startup, plus any bundled
 * ("seed") lessons at {@code curriculum/<subject>/<concept>.md} (lesson + embedded {@code ~~~quiz} block).
 *
 * <ul>
 *   <li>Idempotent: rows matched by slug, unchanged seed lessons skipped by content hash.</li>
 *   <li>Lessons written in the app ({@code content_source = USER}) are never overwritten.</li>
 *   <li>Nothing user progress refers to is deleted — removed topics are archived.</li>
 *   <li>A malformed seed lesson fails startup loudly.</li>
 * </ul>
 */
@Component
public class CurriculumImporter implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CurriculumImporter.class);
    private static final String ROOT = "classpath:curriculum/";

    private final ResourceLoader resources;
    private final PathRepository paths;
    private final SubjectRepository subjects;
    private final ChapterRepository chapters;
    private final ConceptRepository concepts;
    private final LessonParser parser;
    private final LessonWriter writer;

    public CurriculumImporter(ResourceLoader resources, PathRepository paths, SubjectRepository subjects,
                              ChapterRepository chapters, ConceptRepository concepts,
                              LessonParser parser, LessonWriter writer) {
        this.resources = resources;
        this.paths = paths;
        this.subjects = subjects;
        this.chapters = chapters;
        this.concepts = concepts;
        this.parser = parser;
        this.writer = writer;
    }

    @Override
    @Transactional
    public void afterSingletonsInstantiated() {
        Map<String, Object> root = yaml(read(resources.getResource(ROOT + "curriculum.yml")));
        int pathOrder = 0, subjectOrder = 0, chapterOrder = 0, conceptOrder = 0;
        Set<String> seen = new HashSet<>();

        for (Map<String, Object> p : list(root, "paths")) {
            LearningPath path = paths.findBySlug(str(p, "slug")).orElseGet(() -> new LearningPath(str(p, "slug")));
            path.setTitle(str(p, "title"));
            path.setDescription(optStr(p, "description"));
            path.setSortOrder(pathOrder++);
            path = paths.save(path);

            for (Map<String, Object> s : list(p, "subjects")) {
                String subjectSlug = str(s, "slug");
                Subject subject = subjects.findBySlug(subjectSlug).orElseGet(() -> new Subject(subjectSlug));
                subject.setPathId(path.getId());
                subject.setTitle(str(s, "title"));
                subject.setDescription(optStr(s, "description"));
                subject.setSortOrder(subjectOrder++);
                subject = subjects.save(subject);

                for (Map<String, Object> c : list(s, "chapters")) {
                    Long subjectId = subject.getId();
                    Chapter chapter = chapters.findBySubjectIdAndSlug(subjectId, str(c, "slug"))
                            .orElseGet(() -> new Chapter(subjectId, str(c, "slug")));
                    chapter.setTitle(str(c, "title"));
                    chapter.setSortOrder(chapterOrder++);
                    chapter = chapters.save(chapter);

                    for (Object entry : (List<?>) c.getOrDefault("concepts", List.of())) {
                        Map<String, Object> cm = asConceptEntry(entry);
                        String slug = str(cm, "slug");
                        if (!seen.add(slug)) throw new IllegalStateException("Duplicate concept slug: " + slug);
                        importConcept(chapter, subjectSlug, slug, str(cm, "title"), conceptOrder++);
                    }
                }
            }
        }

        for (Concept c : concepts.findAllByStatusNot(Concept.Status.ARCHIVED)) {
            if (!seen.contains(c.getSlug())) {
                c.setStatus(Concept.Status.ARCHIVED);
                log.info("Archived concept '{}' (no longer in curriculum.yml)", c.getSlug());
            }
        }
        long published = concepts.findAllByStatusNot(Concept.Status.ARCHIVED).stream()
                .filter(c -> c.getStatus() == Concept.Status.PUBLISHED).count();
        log.info("Curriculum loaded: {} topics ({} lessons published)", seen.size(), published);
    }

    private void importConcept(Chapter chapter, String subjectSlug, String slug, String title, int order) {
        Concept concept = concepts.findBySlug(slug).orElseGet(() -> new Concept(slug));
        concept.setChapterId(chapter.getId());
        concept.setTitle(title);
        concept.setSortOrder(order);
        if (concept.getStatus() == Concept.Status.ARCHIVED) concept.setStatus(Concept.Status.PLANNED);

        Resource seed = resources.getResource(ROOT + subjectSlug + "/" + slug + ".md");
        if (concept.getContentSource() == Concept.ContentSource.USER || !seed.exists()) {
            concepts.save(concept); // structure only; the lesson (if any) belongs to the user
            return;
        }

        String raw = read(seed);
        ParsedLesson parsed = parser.parse(raw, LessonKind.SECTIONS);
        if (LessonWriter.hash(parsed.source()).equals(concept.getContentHash()) && concept.getStatus() == Concept.Status.PUBLISHED) {
            concepts.save(concept);
            return;
        }
        if (!parsed.valid()) {
            throw new IllegalStateException("Seed lesson " + subjectSlug + "/" + slug + ".md is invalid: " + parsed.errors());
        }
        writer.publish(concept, parsed, Concept.ContentSource.SEED);
        log.debug("Imported seed lesson '{}'", slug);
    }

    // ---------------------------------------------------------------- helpers

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asConceptEntry(Object entry) {
        if (entry instanceof Map<?, ?> m && m.size() == 1 && !m.containsKey("slug")) {
            var e = m.entrySet().iterator().next();
            return Map.of("slug", String.valueOf(e.getKey()), "title", String.valueOf(e.getValue()));
        }
        if (entry instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new IllegalStateException("Invalid concept entry: " + entry);
    }

    private static Map<String, Object> yaml(String text) {
        Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(text);
        if (!(loaded instanceof Map<?, ?> m)) throw new IllegalStateException("Expected a YAML mapping");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) m;
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? List.of() : (List<Map<String, Object>>) v;
    }

    private static String str(Map<String, Object> m, String key) {
        String v = optStr(m, key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing '" + key + "' in " + m.keySet());
        return v;
    }

    private static String optStr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String read(Resource r) {
        try (InputStream in = r.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + r, e);
        }
    }
}
