package com.academy.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GitHubContentClientTest {

    private static final String BASE = "https://api.github.com/repos/me/content/contents/java/strings/demo.md";

    MockRestServiceServer server;
    GitHubContentClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GitHubContentClient(new GitHubProperties("tok", "me/content", "main", null), builder);
    }

    @Test
    void createsNewFileWithBase64ContentAndBearerToken() {
        server.expect(requestTo(BASE + "?ref=main")).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        String expected = Base64.getEncoder().encodeToString("# Hi".getBytes());
        server.expect(requestTo(BASE)).andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer tok"))
                .andExpect(jsonPath("$.content").value(expected))
                .andExpect(jsonPath("$.branch").value("main"))
                .andExpect(jsonPath("$.sha").doesNotExist())
                .andRespond(withSuccess("{\"content\":{\"sha\":\"abc123\"}}", MediaType.APPLICATION_JSON));

        assertThat(client.put("java/strings/demo.md", "# Hi", "Add lesson", null)).isEqualTo("abc123");
        server.verify();
    }

    @Test
    void retriesOnceWithFreshShaWhenKnownShaIsStale() {
        server.expect(requestTo(BASE)).andExpect(method(HttpMethod.PUT)).andExpect(jsonPath("$.sha").value("old"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        server.expect(requestTo(BASE + "?ref=main")).andRespond(withSuccess("{\"sha\":\"current\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE)).andExpect(jsonPath("$.sha").value("current"))
                .andRespond(withSuccess("{\"content\":{\"sha\":\"new\"}}", MediaType.APPLICATION_JSON));

        assertThat(client.put("java/strings/demo.md", "x", "Update", "old")).isEqualTo("new");
        server.verify();
    }

    @Test
    void translatesAuthErrorsIntoReadableMessages() {
        server.expect(requestTo(BASE + "?ref=main")).andRespond(withSuccess("{\"sha\":\"s\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .body("{\"message\":\"Bad credentials\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.put("java/strings/demo.md", "x", "m", null))
                .hasMessageContaining("401").hasMessageContaining("Bad credentials").hasMessageContaining("invalid or expired");
    }

    @Test
    void rejectsUnsafePaths() {
        assertThatThrownBy(() -> client.put("../secrets.md", "x", "m", "sha")).isInstanceOf(IllegalArgumentException.class);
    }
}
