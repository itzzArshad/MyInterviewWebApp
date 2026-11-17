package com.academy;

import com.academy.curriculum.ConceptRepository;
import com.academy.quiz.QuizQuestion;
import com.academy.quiz.QuizQuestionRepository;
import com.academy.revision.ReviewCardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revision, the daily Today session, explain practice, mock interviews and readiness — end to end. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TrainingFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    ConceptRepository concepts;
    @Autowired
    QuizQuestionRepository questions;
    @Autowired
    ReviewCardRepository cards;

    Cookie auth;

    @BeforeEach
    void login() throws Exception {
        auth = TestAuth.login(mvc);
    }

    private JsonNode get(String url) throws Exception {
        return json.readTree(mvc.perform(MockMvcRequestBuildersGet.of(url).cookie(auth)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode post(String url, Object body) throws Exception {
        var req = MockMvcRequestBuildersPost.of(url).cookie(auth);
        if (body != null) req = req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return json.readTree(mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    /** Takes the jdk-jre-jvm quiz, answering q1 wrong and everything else right. */
    private void takeQuizWithOneMistake() throws Exception {
        Long conceptId = concepts.findBySlug("jdk-jre-jvm").orElseThrow().getId();
        List<QuizQuestion> qs = questions.findByConceptIdAndActiveTrueOrderBySortOrder(conceptId);
        long attempt = post("/api/quiz/attempts", java.util.Map.of("conceptSlug", "jdk-jre-jvm")).get("attemptId").asLong();
        for (QuizQuestion q : qs) {
            var body = new java.util.HashMap<String, Object>();
            body.put("questionId", q.getId());
            body.put("hintsUsed", 0);
            if (q.isSelfGraded()) {
                body.put("answer", "x");
                body.put("selfCorrect", true);
            } else {
                body.put("answer", q.getQuestionKey().equals("q1") ? "0" : q.getCorrectAnswer());
            }
            post("/api/quiz/attempts/" + attempt + "/answers", body);
        }
        post("/api/quiz/attempts/" + attempt + "/finish", null);
    }

    @Test
    void quizFeedsRevisionAndMistakeJournal() throws Exception {
        takeQuizWithOneMistake();

        JsonNode mistakes = get("/api/revision/mistakes");
        assertThat(mistakes).hasSize(1);
        assertThat(mistakes.get(0).get("yourAnswer").asText()).startsWith("A.");
        assertThat(mistakes.get(0).get("correctAnswer").asText()).startsWith("C.");

        // nothing due right now: the wrong answer comes back tomorrow, right answers later
        assertThat(get("/api/revision/queue").get("cards")).isEmpty();
        assertThat(cards.findAll()).isNotEmpty();

        // time-travel: make every card due, then review one
        var all = cards.findAll();
        all.forEach(c -> c.setDueAt(OffsetDateTime.now().minusHours(1)));
        cards.saveAll(all);
        JsonNode queue = get("/api/revision/queue");
        assertThat(queue.get("cards").size()).isGreaterThan(0);
        JsonNode card = queue.get("cards").get(0);
        assertThat(card.get("nextIntervals").get("1").asText()).isEqualTo("10m");
        JsonNode graded = post("/api/revision/cards/" + card.get("id").asLong() + "/grade", java.util.Map.of("grade", 3));
        assertThat(graded.get("intervalDays").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(graded.get("stats").get("reviewedToday").asInt()).isEqualTo(1);

        // journal note + resolve
        long mistakeId = mistakes.get(0).get("id").asLong();
        mvc.perform(patch("/api/revision/mistakes/" + mistakeId).cookie(auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Confused JRE with JDK\",\"resolved\":true}")).andExpect(status().isNoContent());
        assertThat(get("/api/revision/mistakes").get(0).get("resolved").asBoolean()).isTrue();

        assertThat(get("/api/revision/summaries").get(0).get("notesMd").asText()).contains("JVM");
    }

    @Test
    void interviewBankComesFromLessonInterviewSections() throws Exception {
        JsonNode bank = get("/api/interview/bank");
        assertThat(bank.size()).isGreaterThan(10);
        String key = bank.get(0).get("key").asText();
        assertThat(key).startsWith("QA-");

        JsonNode q = get("/api/interview/questions/" + key);
        assertThat(q.get("keyPoints").size()).isGreaterThan(0);
        assertThat(q.get("modelAnswer").asText()).isNotBlank();
        assertThat(q.get("autoGraded").asBoolean()).isFalse();

        JsonNode r = post("/api/interview/practice", java.util.Map.of("key", key, "durationSec", 75,
                "covered", List.of(0), "followUpOk", true));
        int total = q.get("keyPoints").size();
        assertThat(r.get("scorePct").asInt()).isEqualTo(Math.round(100f / total));
        assertThat(r.get("nextReview").asText()).isNotBlank();
        assertThat(get("/api/interview/bank").get(0).get("attempts").asInt()).isEqualTo(1);
    }

    @Test
    void todaySessionCompletesFromRealWorkAndBuildsTheStreak() throws Exception {
        JsonNode t = get("/api/today");
        assertThat(t.get("completed").asBoolean()).isFalse();
        assertThat(t.get("streak").asInt()).isZero();
        assertThat(t.get("steps")).hasSize(4);
        assertThat(t.get("explainKey").asText()).startsWith("QA-");
        long drillQ = t.get("drill").get("questionId").asLong();
        String drillConcept = t.get("drill").get("conceptSlug").asText();

        // Think: today's drill
        long drill = post("/api/quiz/attempts", java.util.Map.of("conceptSlug", drillConcept, "questionIds", List.of(drillQ), "mode", "DRILL"))
                .get("attemptId").asLong();
        QuizQuestion dq = questions.findById(drillQ).orElseThrow();
        post("/api/quiz/attempts/" + drill + "/answers", java.util.Map.of("questionId", drillQ, "answer", dq.getCorrectAnswer(), "hintsUsed", 0));
        JsonNode fin = post("/api/quiz/attempts/" + drill + "/finish", null);
        assertThat(fin.get("mastery").asInt()).isZero(); // drills never change mastery

        // Explain: one interview question
        post("/api/interview/practice", java.util.Map.of("key", t.get("explainKey").asText(), "durationSec", 90, "covered", List.of(0, 1)));

        // Revise: nothing due → done. Session complete → streak 1.
        JsonNode after = get("/api/today");
        assertThat(after.get("steps").findValuesAsText("key")).containsExactly("revise", "learn", "think", "explain");
        assertThat(after.get("completed").asBoolean()).isTrue();
        assertThat(after.get("streak").asInt()).isEqualTo(1);
        assertThat(get("/api/dashboard").get("stats").get("streakDays").asInt()).isEqualTo(1);
    }

    @Test
    void mockInterviewProducesReportAndReadiness() throws Exception {
        JsonNode mock = post("/api/interview/mocks", null);
        long id = mock.get("id").asLong();
        JsonNode qs = mock.get("questions");
        assertThat(qs.size()).isBetween(1, 6);

        // answer the first open question well, and an auto-graded one if present
        for (JsonNode q : qs) {
            var body = new java.util.HashMap<String, Object>();
            body.put("key", q.get("key").asText());
            body.put("durationSec", 200);
            if (q.get("autoGraded").asBoolean()) {
                body.put("chosen", "0");
            } else {
                var all = new java.util.ArrayList<Integer>();
                for (int i = 0; i < q.get("keyPoints").size(); i++) all.add(i);
                body.put("covered", all);
                body.put("followUpOk", false);
            }
            post("/api/interview/mocks/" + id + "/answers", body);
            break; // leave the rest unanswered on purpose
        }
        mvc.perform(MockMvcRequestBuildersPost.of("/api/interview/mocks/" + id + "/answers").cookie(auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"" + qs.get(0).get("key").asText() + "\",\"durationSec\":1}"))
                .andExpect(status().isConflict());

        JsonNode report = post("/api/interview/mocks/" + id + "/finish", null);
        assertThat(report.get("scorePct").isNumber()).isTrue();
        assertThat(report.get("items").size()).isEqualTo(qs.size());
        assertThat(report.get("advice").size()).isGreaterThan(0);
        assertThat(report.get("areas").size()).isGreaterThan(0);

        JsonNode hub = get("/api/interview/hub");
        assertThat(hub.get("recentMocks")).hasSize(1);
        JsonNode readiness = hub.get("readiness");
        assertThat(readiness.get("areas")).hasSize(4);
        assertThat(readiness.get("areas").get(0).get("subjectSlug").asText()).isEqualTo("java");
        assertThat(readiness.get("areas").get(0).get("deliveryPct").asInt()).isGreaterThanOrEqualTo(0);
    }

    // small helpers so the static imports above don't clash with this class's get/post methods
    static final class MockMvcRequestBuildersGet {
        static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder of(String url) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url);
        }
    }

    static final class MockMvcRequestBuildersPost {
        static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder of(String url) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url);
        }
    }
}
