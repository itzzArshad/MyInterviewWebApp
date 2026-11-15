package com.academy.quiz;

import com.academy.auth.CurrentUser;
import com.academy.quiz.QuizDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quiz;
    private final CurrentUser currentUser;

    public QuizController(QuizService quiz, CurrentUser currentUser) {
        this.quiz = quiz;
        this.currentUser = currentUser;
    }

    @GetMapping("/concepts/{slug}/questions")
    public List<QuestionView> questions(@PathVariable String slug) {
        return quiz.questions(slug);
    }

    @PostMapping(value = "/attempts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AttemptView start(@Valid @RequestBody StartRequest req, Authentication auth) {
        return quiz.start(currentUser.require(auth).getId(), req);
    }

    @PostMapping(value = "/attempts/{id}/answers", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AnswerResult answer(@PathVariable Long id, @Valid @RequestBody AnswerRequest req, Authentication auth) {
        return quiz.answer(currentUser.require(auth).getId(), id, req);
    }

    @PostMapping("/attempts/{id}/finish")
    public FinishResult finish(@PathVariable Long id, Authentication auth) {
        return quiz.finish(currentUser.require(auth).getId(), id);
    }
}
