package com.academy.lesson;

import com.academy.TestAuth;
import com.academy.curriculum.Concept;
import com.academy.curriculum.ConceptRepository;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LessonAuthoringFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    ConceptRepository concepts;

    Cookie auth;

    static final String LESSON = """
            ---
            difficulty: ADVANCED
            minutes: 20
            summary: Virtual threads from first principles.
            ---
            ## 1. What is this concept?
            Lightweight threads.

            ## 14. Quiz

            ~~~quiz
            questions:
              - key: q1
                type: MCQ
                prompt: Which is true?
                options: ["Platform threads are cheap", "Virtual threads are cheap"]
                answer: B
                explanation: Virtual threads are cheap.
              - key: q2
                type: EXPLAIN
                prompt: Explain pinning.
                explanation: Model answer about pinning.
            ~~~
            """;

    @BeforeEach
    void login() throws Exception {
        auth = TestAuth.login(mvc);
    }

    private String body(String markdown) throws Exception {
        return json.writeValueAsString(Map.of("markdown", markdown));
    }

    @Test
    void promptIsFilledForTheTopicAndKind() throws Exception {
        JsonNode p = json.readTree(mvc.perform(get("/api/lessons/virtual-threads/prompt").cookie(auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String prompt = p.get("prompt").asText();
        assertThat(p.get("kind").asText()).isEqualTo("CONCEPT");
        assertThat(prompt).contains("TOPIC: Virtual threads").contains("Core Java").contains("## 6. Internal working")
                .contains("~~~quiz").doesNotContain("{{");

        String lld = json.readTree(mvc.perform(get("/api/lessons/lld-parking-lot/prompt").cookie(auth))
                .andReturn().getResponse().getContentAsString()).get("prompt").asText();
        assertThat(lld).contains("classDiagram").contains("## 7. Java implementation");
    }

    @Test
    void previewReportsStructureWithoutSaving() throws Exception {
        mvc.perform(post("/api/lessons/virtual-threads/preview").cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(LESSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.questions").value(2))
                .andExpect(jsonPath("$.questionsByType.MCQ").value(1))
                .andExpect(jsonPath("$.warnings[0]").value(org.hamcrest.Matchers.containsString("Missing numbered sections")));
        assertThat(concepts.findBySlug("virtual-threads").orElseThrow().getStatus()).isEqualTo(Concept.Status.PLANNED);
    }

    @Test
    void createReadUpdateDeleteLesson() throws Exception {
        // create
        mvc.perform(put("/api/lessons/virtual-threads").cookie(auth).contentType(MediaType.APPLICATION_JSON).content(body(LESSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").value(2))
                .andExpect(jsonPath("$.sync.state").value("DISABLED"));

        mvc.perform(get("/api/learn/concepts/virtual-threads").cookie(auth))
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.difficulty").value("ADVANCED"))
                .andExpect(jsonPath("$.questionCount").value(2))
                .andExpect(jsonPath("$.bodyMd").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("~~~quiz"))));

        // read back the exact source for editing
        mvc.perform(get("/api/lessons/virtual-threads/source").cookie(auth))
                .andExpect(jsonPath("$.markdown").value(org.hamcrest.Matchers.containsString("~~~quiz")))
                .andExpect(jsonPath("$.contentSource").value("USER"));

        // quiz works on the pasted questions
        mvc.perform(post("/api/quiz/attempts").cookie(auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptSlug\":\"virtual-threads\"}"))
                .andExpect(jsonPath("$.questions.length()").value(2));

        // update: drop q2 → deactivated
        String updated = LESSON.replaceAll("(?s)  - key: q2.*?(?=~~~)", "");
        mvc.perform(put("/api/lessons/virtual-threads").cookie(auth).contentType(MediaType.APPLICATION_JSON).content(body(updated)))
                .andExpect(jsonPath("$.questions").value(1));
        mvc.perform(get("/api/learn/concepts/virtual-threads").cookie(auth)).andExpect(jsonPath("$.questionCount").value(1));

        // delete → topic back to planned, and the seed importer will not resurrect it
        mvc.perform(delete("/api/lessons/virtual-threads").cookie(auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/learn/concepts/virtual-threads").cookie(auth)).andExpect(jsonPath("$.published").value(false));
        Concept c = concepts.findBySlug("virtual-threads").orElseThrow();
        assertThat(c.getContentSource()).isEqualTo(Concept.ContentSource.USER);
        assertThat(c.getSourceMd()).isNull();
    }

    @Test
    void invalidLessonIsRejectedWithReadableErrors() throws Exception {
        mvc.perform(put("/api/lessons/virtual-threads").cookie(auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(LESSON.replace("answer: B", "answer: Z"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("q1")));
        assertThat(concepts.findBySlug("virtual-threads").orElseThrow().getStatus()).isEqualTo(Concept.Status.PLANNED);
    }

    @Test
    void editingASeedLessonMakesItUserOwned() throws Exception {
        String seed = json.readTree(mvc.perform(get("/api/lessons/jdk-jre-jvm/source").cookie(auth))
                .andReturn().getResponse().getContentAsString()).get("markdown").asText();
        assertThat(seed).contains("~~~quiz");
        mvc.perform(put("/api/lessons/jdk-jre-jvm").cookie(auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(seed.replace("JDK, JRE and JVM", "JDK, JRE & JVM"))))
                .andExpect(status().isOk());
        assertThat(concepts.findBySlug("jdk-jre-jvm").orElseThrow().getContentSource()).isEqualTo(Concept.ContentSource.USER);
    }

    @Test
    void githubEndpointsReportDisabled() throws Exception {
        mvc.perform(get("/api/github/status").cookie(auth)).andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(post("/api/github/push-all").cookie(auth)).andExpect(status().isConflict());
        mvc.perform(get("/api/lessons/virtual-threads/source")).andExpect(status().isUnauthorized());
    }
}
