package com.academy.auth;

import com.academy.auth.AuthDtos.LoginRequest;
import com.academy.auth.AuthDtos.MeResponse;
import com.academy.user.ActivityService;
import com.academy.user.AppUser;
import com.academy.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LoginRateLimiter limiter;
    private final ActivityService activity;
    private final CurrentUser currentUser;
    /** Compared against when the username is unknown, so response time does not reveal valid usernames. */
    private final String dummyHash;

    public AuthController(AppUserRepository users, PasswordEncoder encoder, TokenService tokens,
                          LoginRateLimiter limiter, ActivityService activity, CurrentUser currentUser) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.limiter = limiter;
        this.activity = activity;
        this.currentUser = currentUser;
        this.dummyHash = encoder.encode("timing-equaliser-not-a-password");
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String clientKey = http.getRemoteAddr();
        if (limiter.isBlocked(clientKey)) {
            log.warn("Login blocked by rate limiter for {}", clientKey);
            return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again later.");
        }

        Optional<AppUser> user = users.findByUsername(req.username());
        boolean passwordOk = encoder.matches(req.password(), user.map(AppUser::getPasswordHash).orElse(dummyHash));
        if (user.isEmpty() || !passwordOk) {
            limiter.recordFailure(clientKey);
            log.warn("Failed login attempt from {}", clientKey);
            return problem(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }

        limiter.reset(clientKey);
        AppUser u = user.get();
        u.setLastLoginAt(OffsetDateTime.now());
        activity.touchToday(u.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokens.authCookie(tokens.issue(u)).toString())
                .body(new MeResponse(u.getUsername(), u.getDisplayName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, tokens.clearCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        AppUser u = currentUser.require(auth);
        return new MeResponse(u.getUsername(), u.getDisplayName());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
