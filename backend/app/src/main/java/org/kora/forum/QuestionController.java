package org.kora.forum;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    private final QuestionService questions;

    public QuestionController(QuestionService questions) {
        this.questions = questions;
    }

    @GetMapping
    public Page<QuestionService.QuestionSummary> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "unanswered", defaultValue = "false") boolean unanswered,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return questions.search(q, status, unanswered, page, size);
    }

    @GetMapping("/{id}")
    public QuestionService.QuestionDetail get(@PathVariable("id") UUID id) {
        return questions.get(id);
    }

    @PostMapping
    public QuestionService.QuestionDetail create(
            @Valid @RequestBody QuestionService.CreateQuestionRequest request,
            Authentication authentication) {
        return questions.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public QuestionService.QuestionDetail update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody QuestionService.UpdateQuestionRequest request,
            Authentication authentication) {
        return questions.updateQuestion(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/answers")
    public QuestionService.AnswerView answer(
            @PathVariable("id") UUID id,
            @Valid @RequestBody QuestionService.AnswerRequest request,
            Authentication authentication) {
        return questions.answer(authentication.getName(), id, request);
    }

    @PatchMapping("/{id}/answers/{answerId}/accept")
    public QuestionService.QuestionDetail accept(
            @PathVariable("id") UUID id,
            @PathVariable("answerId") UUID answerId,
            Authentication authentication) {
        return questions.acceptAnswer(authentication.getName(), id, answerId);
    }
}
