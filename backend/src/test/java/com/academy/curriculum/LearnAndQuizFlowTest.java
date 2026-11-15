package com.academy.curriculum;

import com.academy.TestAuth;
import com.academy.quiz.QuizQuestion;
import com.academy.quiz.QuizQuestionRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)  // fresh progress per test
class LearnAndQuizFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    ConceptRepository concepts;
    @Autowired
    QuizQuestionRepository questions;

    Cookie auth;

    @BeforeEach
    void login() throws Exception {
        auth = TestAuth.login(mvc);
    }

    private JsonNode getJson(String url) throws Exception {
        return json.readTree(mvc.perform(get(url).cookie(auth)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode postJson(String url, String body) throws Exception {
        var req = post(url).cookie(auth);
        if (body != null) req = req.contentType(MediaType.APPLICATION_JSON).content(body);
        return json.readTree(mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void curriculumIsImportedWithPublishedAndPlannedConcepts() throws Exception {
        JsonNode tree = getJson("/api/learn/tree");
        JsonNode java = tree.get(0).get("subjects").get(0);
        assertThat(java.get("slug").asText()).isEqualTo("java");
        assertThat(java.get("published").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(java.get("total").asInt()).isGreaterThan(java.get("published").asInt());
        assertThat(tree.get(0).get("subjects")).hasSize(4);

        // every published lesson has the 13 structured sections and a quiz
        for (Concept c : concepts.findAll()) {
            if (c.getStatus() != Concept.Status.PUBLISHED) continue;
            for (int i = 1; i <= 13; i++) {
                assertThat(c.getBodyMd()).as(c.getSlug() + " section " + i).contains("## " + i + ". ");
            }
            assertThat(questions.countByConceptIdAndActiveTrue(c.getId())).as(c.getSlug() + " quiz").isGreaterThanOrEqualTo(5);
        }
    }

    @Test
    void lessonDetailHasNavigationAndRelatedLinks() throws Exception {
        JsonNode c = getJson("/api/learn/concepts/hashmap-internals");
        assertThat(c.get("published").asBoolean()).isTrue();
        assertThat(c.get("bodyMd").asText()).contains("## 6. Internal working");
        assertThat(c.get("subject").get("slug").asText()).isEqualTo("java");
        assertThat(c.get("prev").get("slug").asText()).isEqualTo("equals-hashcode");
        assertThat(c.get("related").findValuesAsText("slug")).contains("equals-hashcode");
        assertThat(c.get("questionCount").asInt()).isGreaterThan(5);
    }

    @Test
    void plannedConceptIsVisibleButCannotBeTracked() throws Exception {
        JsonNode c = getJson("/api/learn/concepts/virtual-threads");
        assertThat(c.get("published").asBoolean()).isFalse();
        mvc.perform(post("/api/learn/concepts/virtual-threads/view").cookie(auth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/learn/concepts/does-not-exist").cookie(auth)).andExpect(status().isNotFound());
    }

    @Test
    void progressBookmarkAndCompletionFlowIntoDashboard() throws Exception {
        assertThat(postJson("/api/learn/concepts/jdk-jre-jvm/view", null).get("status").asText()).isEqualTo("IN_PROGRESS");

        mvc.perform(put("/api/learn/concepts/jdk-jre-jvm/progress").cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true,\"bookmarked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.bookmarked").value(true));

        JsonNode dash = getJson("/api/dashboard");
        assertThat(dash.get("stats").get("conceptsCompleted").asInt()).isEqualTo(1);
        assertThat(dash.get("bookmarks").get(0).get("slug").asText()).isEqualTo("jdk-jre-jvm");
        // next unfinished lesson is suggested
        assertThat(dash.get("continueLearning").get("slug").asText()).isNotEqualTo("jdk-jre-jvm");
    }

    @Test
    void quizAttemptIsGradedServerSideAndUpdatesMastery() throws Exception {
        Long conceptId = concepts.findBySlug("jdk-jre-jvm").orElseThrow().getId();
        List<QuizQuestion> qs = questions.findByConceptIdAndActiveTrueOrderBySortOrder(conceptId);

        JsonNode attempt = postJson("/api/quiz/attempts", "{\"conceptSlug\":\"jdk-jre-jvm\"}");
        long attemptId = attempt.get("attemptId").asLong();
        assertThat(attempt.get("questions")).hasSize(qs.size());
        // answers are never sent before answering
        assertThat(attempt.toString()).doesNotContain("correctAnswer");
        // ordered easy → hard
        assertThat(attempt.get("questions").get(0).get("difficulty").asText()).isEqualTo("BEGINNER");

        int expectedScore = 0;
        for (QuizQuestion q : qs) {
            String body;
            if (q.isSelfGraded()) {
                body = "{\"questionId\":%d,\"answer\":\"my explanation\",\"hintsUsed\":0,\"selfCorrect\":true}".formatted(q.getId());
                expectedScore++;
            } else if (q.getQuestionKey().equals("q1")) {
                body = "{\"questionId\":%d,\"answer\":\"0\",\"hintsUsed\":0}".formatted(q.getId());  // wrong on purpose
            } else {
                body = "{\"questionId\":%d,\"answer\":%s,\"hintsUsed\":1}".formatted(q.getId(), json.writeValueAsString(q.getCorrectAnswer()));
                expectedScore++;
            }
            JsonNode result = postJson("/api/quiz/attempts/" + attemptId + "/answers", body);
            assertThat(result.get("explanation").asText()).isNotBlank();
        }

        // answering twice is rejected
        mvc.perform(post("/api/quiz/attempts/" + attemptId + "/answers").cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":%d,\"answer\":\"2\",\"hintsUsed\":0}".formatted(qs.get(0).getId())))
                .andExpect(status().isConflict());

        JsonNode fin = postJson("/api/quiz/attempts/" + attemptId + "/finish", null);
        assertThat(fin.get("score").asInt()).isEqualTo(expectedScore);
        assertThat(fin.get("total").asInt()).isEqualTo(qs.size());
        assertThat(fin.get("incorrectQuestionIds")).hasSize(1);
        assertThat(fin.get("mastery").asInt()).isBetween(1, 99);

        // finished attempts are closed
        mvc.perform(post("/api/quiz/attempts/" + attemptId + "/finish").cookie(auth)).andExpect(status().isConflict());

        // retry only the incorrect question
        long wrongId = fin.get("incorrectQuestionIds").get(0).asLong();
        JsonNode retry = postJson("/api/quiz/attempts", "{\"conceptSlug\":\"jdk-jre-jvm\",\"questionIds\":[" + wrongId + "]}");
        assertThat(retry.get("questions")).hasSize(1);

        JsonNode dash = getJson("/api/dashboard");
        assertThat(dash.get("stats").get("quizzesTaken").asInt()).isEqualTo(1);
        assertThat(dash.get("stats").get("quizAveragePct").asInt()).isGreaterThan(0);

        JsonNode lesson = getJson("/api/learn/concepts/jdk-jre-jvm");
        assertThat(lesson.get("lastQuiz").get("score").asInt()).isEqualTo(expectedScore);
    }

    @Test
    void cannotAnswerQuestionsOutsideTheAttempt() throws Exception {
        long attemptId = postJson("/api/quiz/attempts", "{\"conceptSlug\":\"jdk-jre-jvm\"}").get("attemptId").asLong();
        Long otherConcept = concepts.findBySlug("hashmap-internals").orElseThrow().getId();
        Long foreignQ = questions.findByConceptIdAndActiveTrueOrderBySortOrder(otherConcept).get(0).getId();
        mvc.perform(post("/api/quiz/attempts/" + attemptId + "/answers").cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":%d,\"answer\":\"1\",\"hintsUsed\":0}".formatted(foreignQ)))
                .andExpect(status().isBadRequest());
    }
}
