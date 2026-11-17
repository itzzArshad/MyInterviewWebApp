package com.academy.interview;

import com.academy.auth.CurrentUser;
import com.academy.interview.InterviewDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interview;
    private final CurrentUser currentUser;

    public InterviewController(InterviewService interview, CurrentUser currentUser) {
        this.interview = interview;
        this.currentUser = currentUser;
    }

    private Long uid(Authentication auth) {
        return currentUser.require(auth).getId();
    }

    @GetMapping("/hub")
    public Hub hub(Authentication auth) {
        return interview.hub(uid(auth));
    }

    @GetMapping("/readiness")
    public Readiness readiness(Authentication auth) {
        return interview.readiness(uid(auth));
    }

    @GetMapping("/bank")
    public List<BankItem> bank(Authentication auth) {
        return interview.bank(uid(auth));
    }

    @GetMapping("/questions/{key}")
    public Question question(@PathVariable String key) {
        return interview.question(key);
    }

    @GetMapping("/next-explain")
    public ResponseEntity<Map<String, String>> nextExplain(Authentication auth) {
        String key = interview.nextExplainKey(uid(auth));
        return key == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(Map.of("key", key));
    }

    @PostMapping(value = "/practice", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PracticeResult practise(@Valid @RequestBody PracticeRequest req, Authentication auth) {
        return interview.practise(uid(auth), req);
    }

    @PostMapping("/mocks")
    public MockView createMock(Authentication auth) {
        return interview.createMock(uid(auth));
    }

    @GetMapping("/mocks")
    public List<MockSummary> recentMocks(Authentication auth) {
        return interview.recentMocks(uid(auth));
    }

    @GetMapping("/mocks/{id}")
    public MockView mock(@PathVariable Long id, Authentication auth) {
        return interview.mock(uid(auth), id);
    }

    @PostMapping(value = "/mocks/{id}/answers", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PracticeResult answerMock(@PathVariable Long id, @Valid @RequestBody PracticeRequest req, Authentication auth) {
        return interview.answerMock(uid(auth), id, req);
    }

    @PostMapping("/mocks/{id}/finish")
    public MockReport finishMock(@PathVariable Long id, Authentication auth) {
        return interview.finishMock(uid(auth), id);
    }

    @GetMapping("/mocks/{id}/report")
    public MockReport report(@PathVariable Long id, Authentication auth) {
        return interview.report(uid(auth), id);
    }
}
