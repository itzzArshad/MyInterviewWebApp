package com.academy.curriculum;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Loads the (small) curriculum hierarchy once per request so services can label topics cheaply. */
@Component
public class CurriculumIndex {

    public record Topic(Long id, String slug, String title, boolean published, Long chapterId,
                        String chapterSlug, String chapterTitle, String subjectSlug, String subjectTitle, int order) {
    }

    public record Snapshot(Map<Long, Topic> byId, List<Subject> subjects) {
        public Topic topic(Long conceptId) {
            return byId.get(conceptId);
        }

        public List<Topic> inSubject(String subjectSlug) {
            return byId.values().stream().filter(t -> t.subjectSlug().equals(subjectSlug))
                    .sorted(Comparator.comparingInt(Topic::order)).toList();
        }
    }

    private final ConceptRepository concepts;
    private final ChapterRepository chapters;
    private final SubjectRepository subjects;

    public CurriculumIndex(ConceptRepository concepts, ChapterRepository chapters, SubjectRepository subjects) {
        this.concepts = concepts;
        this.chapters = chapters;
        this.subjects = subjects;
    }

    public Snapshot load() {
        Map<Long, Chapter> ch = chapters.findAll().stream().collect(Collectors.toMap(Chapter::getId, Function.identity()));
        List<Subject> subjectList = subjects.findAllByOrderBySortOrder();
        Map<Long, Subject> sub = subjectList.stream().collect(Collectors.toMap(Subject::getId, Function.identity()));
        Map<Long, Topic> topics = new LinkedHashMap<>();
        for (ConceptSummary s : concepts.findAllSummaries()) {
            Chapter c = ch.get(s.chapterId());
            Subject su = c == null ? null : sub.get(c.getSubjectId());
            if (c == null || su == null) continue;
            topics.put(s.id(), new Topic(s.id(), s.slug(), s.title(), s.published(), c.getId(), c.getSlug(), c.getTitle(),
                    su.getSlug(), su.getTitle(), s.sortOrder()));
        }
        return new Snapshot(topics, subjectList);
    }
}
