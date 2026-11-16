package com.academy.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional: where lessons are mirrored. Sync is disabled when token or repo is blank.
 *
 * @param token  fine-grained personal access token with "Contents: read and write" on {@code repo} only
 * @param repo   "owner/name", e.g. itzzArshad/academy-content
 * @param branch branch to commit to
 */
@ConfigurationProperties(prefix = "academy.github")
public record GitHubProperties(String token, String repo, String branch, String apiUrl) {

    public GitHubProperties {
        branch = branch == null || branch.isBlank() ? "main" : branch;
        apiUrl = apiUrl == null || apiUrl.isBlank() ? "https://api.github.com" : apiUrl;
        if (repo != null && !repo.isBlank() && !repo.matches("[\\w.-]+/[\\w.-]+")) {
            throw new IllegalArgumentException("academy.github.repo must look like owner/name");
        }
    }

    public boolean enabled() {
        return token != null && !token.isBlank() && repo != null && !repo.isBlank();
    }

    public String webUrl(String path) {
        return "https://github.com/" + repo + "/blob/" + branch + "/" + path;
    }
}
