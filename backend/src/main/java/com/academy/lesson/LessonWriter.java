package com.academy.lesson;

import com.academy.curriculum.Concept;
import com.academy.curriculum.ConceptRepository;
import com.academy.lesson.LessonParser.ParsedLesson;
import com.academy.lesson.LessonParser.ParsedQuestion;
import com.academy.quiz.QuizQuestion;
import com.academy.quiz.QuizQuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Applies a parsed lesson to the database: lesson fields on the concept, and quiz questions
 * upserted by key (so attempt history keeps pointing at the same rows; removed questions are
 * deactivated, never deleted).
 */
@Component
public class LessonWriter {

    private final ConceptRepository concepts;
    private final QuizQuestionRepository questions;
    private final ObjectMapper json;

    public LessonWriter(ConceptRepository concepts, QuizQuestionRepository questions, ObjectMapper json) {
        this.concepts = concepts;
        this.questions = questions;
        this.json = json;
    }

    public Concept publish(Concept c, ParsedLesson p, Concept.ContentSource source) {
        if (!p.valid()) throw new IllegalArgumentException("Cannot publish an invalid lesson: " + p.errors());
        c.setBodyMd(p.body());
        c.setSourceMd(p.source());
        if (p.difficulty() != null) c.setDifficulty(p.difficulty());
        c.setEstMinutes(p.minutes() != null && p.minutes() > 0 ? p.minutes() : estimateMinutes(p.body()));
        c.setSummary(p.summary());
        if (!p.related().isEmpty()) c.setRelatedSlugs(String.join(",", p.related()));
        c.setStatus(Concept.Status.PUBLISHED);
        c.setContentSource(source);
        c.setContentHash(hash(p.source()));
        c.setUpdatedAt(OffsetDateTime.now());
        Concept saved = concepts.save(c);
        writeQuestions(saved.getId(), p.questions());
        return saved;
    }

    /** Removes the lesson content but keeps the topic, progress and quiz history. */
    public Concept unpublish(Concept c) {
        c.setStatus(Concept.Status.PLANNED);
        c.setBodyMd(null);
        c.setSourceMd(null);
        c.setSummary(null);
        c.setContentHash(null);
        c.setContentSource(Concept.ContentSource.USER); // a deleted seed lesson must not come back on restart
        c.setUpdatedAt(OffsetDateTime.now());
        questions.findByConceptId(c.getId()).forEach(q -> q.setActive(false));
        return concepts.save(c);
    }

    private void writeQuestions(Long conceptId, List<ParsedQuestion> parsed) {
        Map<String, QuizQuestion> existing = new HashMap<>();
        questions.findByConceptId(conceptId).forEach(q -> existing.put(q.getQuestionKey(), q));
        Set<String> seen = new HashSet<>();
        int order = 0;
        for (ParsedQuestion pq : parsed) {
            seen.add(pq.key());
            QuizQuestion q = existing.getOrDefault(pq.key(), new QuizQuestion(conceptId, pq.key()));
            q.setType(pq.type());
            q.setDifficulty(pq.difficulty());
            q.setPromptMd(pq.prompt());
            q.setCodeSnippet(pq.code());
            q.setOptionsJson(pq.options() == null ? null : toJson(pq.options()));
            q.setHintsJson(pq.hints().isEmpty() ? null : toJson(pq.hints()));
            q.setCorrectAnswer(pq.correctAnswer());
            q.setExplanationMd(pq.explanation());
            q.setSortOrder(order++);
            q.setActive(true);
            questions.save(q);
        }
        existing.values().stream().filter(q -> !seen.contains(q.getQuestionKey())).forEach(q -> q.setActive(false));
    }

    static int estimateMinutes(String body) {
        int words = body.isBlank() ? 0 : body.split("\\s+").length;
        return Math.max(5, Math.round(words / 200f));
    }

    public static String hash(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
