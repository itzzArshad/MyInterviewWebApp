package com.academy.lesson;

import com.academy.lesson.LessonDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LessonController {

    private final LessonService lessons;

    public LessonController(LessonService lessons) {
        this.lessons = lessons;
    }

    @GetMapping("/lessons/{slug}/source")
    public Source source(@PathVariable String slug) {
        return lessons.source(slug);
    }

    @GetMapping("/lessons/{slug}/prompt")
    public Prompt prompt(@PathVariable String slug) {
        return lessons.prompt(slug);
    }

    @PostMapping(value = "/lessons/{slug}/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Preview preview(@PathVariable String slug, @Valid @RequestBody MarkdownRequest body) {
        return lessons.preview(slug, body.markdown());
    }

    @PutMapping(value = "/lessons/{slug}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SaveResult save(@PathVariable String slug, @Valid @RequestBody MarkdownRequest body) {
        return lessons.save(slug, body.markdown());
    }

    @DeleteMapping("/lessons/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        lessons.delete(slug);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lessons/{slug}/sync")
    public SyncView sync(@PathVariable String slug) {
        return lessons.sync(slug);
    }

    @GetMapping("/github/status")
    public GitHubStatus githubStatus() {
        return lessons.githubStatus();
    }

    @PostMapping("/github/push-all")
    public BulkResult pushAll() {
        return lessons.pushAll();
    }

    @PostMapping("/github/pull")
    public BulkResult pull() {
        return lessons.pullAll();
    }
}
