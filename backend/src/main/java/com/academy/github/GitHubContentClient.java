package com.academy.github;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Minimal GitHub REST client for one content repository (contents + git trees APIs).
 * Every write is a normal commit, so the repo keeps full history of your lessons.
 */
@Component
public class GitHubContentClient {

    public static class GitHubException extends RuntimeException {
        public GitHubException(String message) {
            super(message);
        }
    }

    private final GitHubProperties props;
    private final RestClient http;

    public GitHubContentClient(GitHubProperties props, RestClient.Builder builder) {
        this.props = props;
        this.http = builder
                .baseUrl(props.apiUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "backend-academy")
                .defaultHeaders(h -> {
                    if (props.enabled()) h.setBearerAuth(props.token());
                })
                .build();
    }

    public boolean enabled() {
        return props.enabled();
    }

    public GitHubProperties properties() {
        return props;
    }

    /** Creates or updates a file. Returns the new blob SHA. Retries once if our known SHA is stale. */
    public String put(String path, String content, String message, String knownSha) {
        requireEnabled();
        String sha = knownSha != null ? knownSha : currentSha(path);
        try {
            return doPut(path, content, message, sha);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 422) {
                return wrap(() -> doPut(path, content, message, currentSha(path)));
            }
            throw translate(e);
        }
    }

    /** Deletes a file if it exists. */
    public void delete(String path, String message) {
        requireEnabled();
        String sha = currentSha(path);
        if (sha == null) return;
        wrap(() -> http.method(HttpMethod.DELETE).uri(contentsUri(path))
                .body(Map.of("message", message, "sha", sha, "branch", props.branch()))
                .retrieve().toBodilessEntity());
    }

    /** Paths of all files in the branch ending with {@code suffix}, mapped to their blob SHA. */
    public Map<String, String> listFiles(String suffix) {
        requireEnabled();
        Map<?, ?> tree = wrap(() -> http.get().uri("/repos/" + props.repo() + "/git/trees/" + props.branch() + "?recursive=1")
                .retrieve().body(Map.class));
        Map<String, String> files = new TreeMap<>();
        if (tree != null && tree.get("tree") instanceof List<?> entries) {
            for (Object o : entries) {
                if (o instanceof Map<?, ?> e && "blob".equals(e.get("type")) && String.valueOf(e.get("path")).endsWith(suffix)) {
                    files.put(String.valueOf(e.get("path")), String.valueOf(e.get("sha")));
                }
            }
        }
        return files;
    }

    public String read(String path) {
        requireEnabled();
        byte[] bytes = wrap(() -> http.get().uri(contentsUri(path) + "?ref=" + props.branch())
                .header("Accept", "application/vnd.github.raw+json")
                .retrieve().body(byte[].class));
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- internals

    private String doPut(String path, String content, String message, String sha) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
        body.put("branch", props.branch());
        if (sha != null) body.put("sha", sha);
        Map<?, ?> res = http.put().uri(contentsUri(path)).body(body).retrieve().body(Map.class);
        if (res != null && res.get("content") instanceof Map<?, ?> c && c.get("sha") != null) return String.valueOf(c.get("sha"));
        throw new GitHubException("GitHub did not return a file SHA");
    }

    private String currentSha(String path) {
        return wrap(() -> http.get().uri(contentsUri(path) + "?ref=" + props.branch()).exchange((req, res) -> {
            HttpStatusCode status = res.getStatusCode();
            if (status.value() == 404) return null;
            if (status.isError()) throw new GitHubException("GitHub " + status.value() + " reading " + path);
            Map<?, ?> m = res.bodyTo(Map.class);
            return m == null || m.get("sha") == null ? null : String.valueOf(m.get("sha"));
        }));
    }

    private String contentsUri(String path) {
        if (!path.matches("[\\w./-]+") || path.contains("..")) throw new IllegalArgumentException("Unsafe path: " + path);
        return "/repos/" + props.repo() + "/contents/" + path;
    }

    private void requireEnabled() {
        if (!props.enabled()) throw new GitHubException("GitHub sync is not configured (GITHUB_CONTENT_TOKEN / GITHUB_CONTENT_REPO).");
    }

    private static <T> T wrap(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw translate(e);
        }
    }

    private static GitHubException translate(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        String msg = body.contains("\"message\"") ? body.replaceAll("(?s).*\"message\"\\s*:\\s*\"([^\"]*)\".*", "$1") : e.getStatusText();
        String hint = switch (e.getStatusCode().value()) {
            case 401 -> " — the token is invalid or expired.";
            case 403 -> " — the token lacks 'Contents: read and write' on this repo.";
            case 404 -> " — repo or branch not found (does the token have access to it?).";
            default -> "";
        };
        return new GitHubException("GitHub " + e.getStatusCode().value() + ": " + msg + hint);
    }
}
