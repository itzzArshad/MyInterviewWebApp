package com.academy.curriculum;

import com.academy.auth.CurrentUser;
import com.academy.curriculum.LearnDtos.ConceptDetail;
import com.academy.curriculum.LearnDtos.PathNode;
import com.academy.curriculum.LearnDtos.ProgressUpdate;
import com.academy.curriculum.LearnDtos.ProgressView;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learn")
public class LearnController {

    private final LearnService learn;
    private final CurrentUser currentUser;

    public LearnController(LearnService learn, CurrentUser currentUser) {
        this.learn = learn;
        this.currentUser = currentUser;
    }

    @GetMapping("/tree")
    public List<PathNode> tree(Authentication auth) {
        return learn.tree(currentUser.require(auth).getId());
    }

    @GetMapping("/concepts/{slug}")
    public ConceptDetail concept(@PathVariable String slug, Authentication auth) {
        return learn.concept(currentUser.require(auth).getId(), slug);
    }

    @PostMapping("/concepts/{slug}/view")
    public ProgressView view(@PathVariable String slug, Authentication auth) {
        return learn.markViewed(currentUser.require(auth).getId(), slug);
    }

    @PutMapping(value = "/concepts/{slug}/progress", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProgressView progress(@PathVariable String slug, @RequestBody ProgressUpdate body, Authentication auth) {
        return learn.update(currentUser.require(auth).getId(), slug, body);
    }
}
