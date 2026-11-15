package com.academy.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * All security-relevant settings come from environment variables in production.
 * Startup fails fast if any of them is missing or too weak.
 */
@Validated
@ConfigurationProperties(prefix = "academy")
public record AcademyProperties(@Valid @NotNull Jwt jwt,
                                @Valid @NotNull Owner owner,
                                @Valid @NotNull Cookie cookie,
                                @Valid @NotNull LoginLimit loginLimit) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "academy.jwt.secret must be at least 32 characters") String secret,
            @NotNull Duration ttl) {
    }

    /** The single authorised user. Password is hashed with BCrypt at startup and never stored in plain text. */
    public record Owner(@NotBlank String username, @NotBlank @Size(min = 8) String password, String displayName) {
    }

    public record Cookie(@NotBlank String name, boolean secure) {
    }

    public record LoginLimit(@Min(1) int maxFailures, @NotNull Duration window) {
    }
}
