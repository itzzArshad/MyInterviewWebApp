package com.academy.revision;

import com.academy.auth.CurrentUser;
import com.academy.revision.RevisionDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/revision")
public class RevisionController {

    private final ReviewService reviews;
    private final CurrentUser currentUser;

    public RevisionController(ReviewService reviews, CurrentUser currentUser) {
        this.reviews = reviews;
        this.currentUser = currentUser;
    }

    @GetMapping("/queue")
    public ReviewQueue queue(@RequestParam(defaultValue = "20") int limit, Authentication auth) {
        return reviews.queue(currentUser.require(auth).getId(), limit);
    }

    @PostMapping(value = "/cards/{id}/grade", consumes = MediaType.APPLICATION_JSON_VALUE)
    public GradeResult grade(@PathVariable Long id, @Valid @RequestBody GradeRequest req, Authentication auth) {
        return reviews.grade(currentUser.require(auth).getId(), id, req.grade());
    }

    @GetMapping("/stats")
    public Stats stats(Authentication auth) {
        return reviews.stats(currentUser.require(auth).getId());
    }

    @GetMapping("/mistakes")
    public List<MistakeView> mistakes(Authentication auth) {
        return reviews.mistakes(currentUser.require(auth).getId());
    }

    @PatchMapping(value = "/mistakes/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateMistake(@PathVariable Long id, @Valid @RequestBody MistakeUpdate req, Authentication auth) {
        reviews.updateMistake(currentUser.require(auth).getId(), id, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summaries")
    public List<SummaryView> summaries(Authentication auth) {
        return reviews.summaries(currentUser.require(auth).getId());
    }
}
