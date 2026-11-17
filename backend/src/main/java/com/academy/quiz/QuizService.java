package com.academy.quiz;

import com.academy.curriculum.Concept;
import com.academy.curriculum.LearnService;
import com.academy.progress.ConceptProgress;
import com.academy.progress.ConceptProgressRepository;
import com.academy.quiz.QuizDtos.*;
import com.academy.common.ProgressEvent;
import com.academy.revision.ReviewService;
import com.academy.user.ActivityService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
public class QuizService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final QuizAnswerRepository answers;
    private final ConceptProgressRepository progressRepo;
    private final LearnService learn;
    private final ActivityService activity;
    private final ReviewService reviews;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;

    public QuizService(QuizQuestionRepository questions, QuizAttemptRepository attempts, QuizAnswerRepository answers,
                       ConceptProgressRepository progressRepo, LearnService learn,
                       ActivityService activity, ReviewService reviews, ApplicationEventPublisher events, ObjectMapper json) {
        this.reviews = reviews;
        this.events = events;
        this.questions = questions;
        this.attempts = attempts;
        this.answers = answers;
        this.progressRepo = progressRepo;
        this.learn = learn;
        this.activity = activity;
        this.json = json;
    }

    /**
     * Starts an attempt over all active questions of a concept, or over a subset
     * (used for "retry the ones I got wrong"). Questions are ordered easy → hard.
     */
    @Transactional
    public AttemptView start(Long userId, StartRequest req) {
        Concept concept = learn.requirePublished(req.conceptSlug());
        List<QuizQuestion> pool = questions.findByConceptIdAndActiveTrueOrderBySortOrder(concept.getId());
        if (req.questionIds() != null && !req.questionIds().isEmpty()) {
            Set<Long> wanted = new HashSet<>(req.questionIds());
            pool = pool.stream().filter(q -> wanted.contains(q.getId())).toList();
        }
        if (pool.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "This concept has no quiz questions yet.");

        List<QuizQuestion> ordered = pool.stream()
                .sorted(Comparator.comparing(QuizQuestion::getDifficulty).thenComparingInt(QuizQuestion::getSortOrder))
                .toList();
        String mode = "DRILL".equalsIgnoreCase(req.mode()) ? "DRILL"
                : req.questionIds() != null && !req.questionIds().isEmpty() ? "RETRY" : "LESSON";
        QuizAttempt attempt = attempts.save(new QuizAttempt(userId, concept.getId(),
                ordered.stream().map(QuizQuestion::getId).toList(), mode));
        return new AttemptView(attempt.getId(), concept.getSlug(), ordered.stream().map(this::view).toList());
    }

    /** Question previews for a concept (no answers) — lets the UI pre-render code snippets. */
    @Transactional(readOnly = true)
    public List<QuestionView> questions(String conceptSlug) {
        Concept concept = learn.requirePublished(conceptSlug);
        return questions.findByConceptIdAndActiveTrueOrderBySortOrder(concept.getId()).stream().map(this::view).toList();
    }

    @Transactional
    public AnswerResult answer(Long userId, Long attemptId, AnswerRequest req) {
        QuizAttempt attempt = requireOpenAttempt(userId, attemptId);
        if (!attempt.questionIdList().contains(req.questionId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Question is not part of this attempt.");
        }
        if (answers.existsByAttemptIdAndQuestionId(attemptId, req.questionId())) {
            throw new ResponseStatusException(CONFLICT, "Question already answered in this attempt.");
        }
        QuizQuestion q = questions.findById(req.questionId()).orElseThrow();

        boolean correct;
        if (q.isSelfGraded()) {
            if (req.selfCorrect() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Self-graded questions need selfCorrect.");
            }
            correct = req.selfCorrect();
        } else {
            correct = req.answer() != null && Grader.matches(q, req.answer());
        }
        answers.save(new QuizAnswer(attemptId, q.getId(), req.answer(), correct, req.hintsUsed()));
        reviews.onQuizAnswered(userId, q, correct, req.answer());
        return new AnswerResult(correct, q.getCorrectAnswer(), q.getExplanationMd());
    }

    /** Scores the attempt and updates concept mastery. Unanswered questions count as wrong. */
    @Transactional
    public FinishResult finish(Long userId, Long attemptId) {
        QuizAttempt attempt = requireOpenAttempt(userId, attemptId);
        Map<Long, QuizAnswer> given = answers.findByAttemptId(attemptId).stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity()));

        int score = 0;
        double weighted = 0;
        List<Long> incorrect = new ArrayList<>();
        for (Long qid : attempt.questionIdList()) {
            QuizAnswer a = given.get(qid);
            if (a != null && a.isCorrect()) {
                score++;
                weighted += Grader.credit(a.getHintsUsed());
            } else {
                incorrect.add(qid);
            }
        }
        int total = attempt.getTotal();
        int pct = Math.round(100f * score / total);
        int weightedPct = (int) Math.round(100 * weighted / total);

        boolean hadPrior = attempts.existsByUserIdAndConceptIdAndFinishedAtIsNotNull(userId, attempt.getConceptId());
        attempt.finish(score);

        ConceptProgress p = learn.progressFor(userId, attempt.getConceptId());
        p.markViewed();
        // A one-question daily drill is practice, not a measurement — it must not swing concept mastery.
        if (!"DRILL".equals(attempt.getMode())) {
            p.setMastery(Grader.nextMastery(hadPrior ? p.getMastery() : null, weightedPct));
        }
        progressRepo.save(p);
        activity.recordCompletion(userId);
        events.publishEvent(new ProgressEvent(userId));

        return new FinishResult(score, total, pct, p.getMastery(), incorrect);
    }

    private QuizAttempt requireOpenAttempt(Long userId, Long attemptId) {
        QuizAttempt a = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Attempt not found"));
        if (a.isFinished()) throw new ResponseStatusException(CONFLICT, "Attempt already finished.");
        return a;
    }

    private QuestionView view(QuizQuestion q) {
        return new QuestionView(q.getId(), q.getType(), q.getDifficulty(), q.getPromptMd(), q.getCodeSnippet(),
                parse(q.getOptionsJson()), parse(q.getHintsJson()), q.isSelfGraded(),
                q.isSelfGraded() ? q.getExplanationMd() : null);
    }

    private List<String> parse(String jsonArray) {
        if (jsonArray == null) return null;
        try {
            return json.readValue(jsonArray, STRING_LIST);
        } catch (Exception e) {
            throw new IllegalStateException("Corrupt quiz JSON", e);
        }
    }

    /** Pure grading rules — kept separate so they are trivially unit-testable. */
    public static final class Grader {
        private Grader() {
        }

        public static boolean matches(QuizQuestion q, String given) {
            String expected = q.getCorrectAnswer();
            if (q.getType() == QuizQuestion.Type.TRUE_FALSE || q.getOptionsJson() != null) {
                return expected.equals(given.strip().toLowerCase(Locale.ROOT));
            }
            return normaliseOutput(expected).equals(normaliseOutput(given));
        }

        /** Output comparison ignores trailing spaces, blank edges and Windows line endings — not content. */
        static String normaliseOutput(String s) {
            return s.replace("\r\n", "\n").lines().map(String::stripTrailing)
                    .collect(Collectors.joining("\n")).strip();
        }

        /** A correct answer is worth less the more hints it needed (never below half). */
        public static double credit(int hintsUsed) {
            return Math.max(0.5, 1.0 - 0.25 * hintsUsed);
        }

        /** Mastery moves towards the latest result but remembers history (exponential smoothing). */
        public static int nextMastery(Integer previous, int latestPct) {
            return previous == null ? latestPct : (int) Math.round(0.4 * previous + 0.6 * latestPct);
        }
    }
}
