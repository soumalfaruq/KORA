package org.kora.forum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kora.auth.Member;
import org.kora.auth.MemberRepository;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class QuestionServiceTest {
    @Mock
    private QuestionRepository questions;

    @Mock
    private AnswerRepository answers;

    @Mock
    private MemberRepository members;

    private QuestionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new QuestionService(questions, answers, members);
    }

    @Test
    void onlyQuestionAuthorCanAcceptAnswer() {
        Member author = member("author");
        Member other = member("other");
        Question question = new Question("Question", "Body", author, new LinkedHashSet<>());
        Answer answer = new Answer(question, other, "Answer");
        when(questions.findById(question.getId())).thenReturn(Optional.of(question));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.acceptAnswer("other", question.getId(), answer.getId()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void questionAuthorCanEditQuestionAndNormalizeTags() {
        Member author = member("author");
        Question question = new Question("Question", "Body", author, new LinkedHashSet<>(List.of("old-tag")));
        when(questions.findById(question.getId())).thenReturn(Optional.of(question));
        var createdAt = question.getCreatedAt();
        var updatedAt = question.getUpdatedAt();

        QuestionService.QuestionDetail result = service.updateQuestion(
                "author",
                question.getId(),
                new QuestionService.UpdateQuestionRequest(
                        "  Updated question  ",
                        "  Updated body  ",
                        List.of(" Java ", "java", "spring-boot")));

        assertEquals("Updated question", result.title());
        assertEquals("Updated body", result.body());
        assertEquals(List.of("java", "spring-boot"), result.tags());
        assertEquals(createdAt, question.getCreatedAt());
        assertEquals(QuestionStatus.OPEN, result.status());
        assertTrue(result.updatedAt().compareTo(updatedAt) >= 0);
    }

    @Test
    void onlyQuestionAuthorCanEditQuestion() {
        Member author = member("author");
        Question question = new Question("Question", "Body", author, new LinkedHashSet<>());
        when(questions.findById(question.getId())).thenReturn(Optional.of(question));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateQuestion(
                        "other",
                        question.getId(),
                        new QuestionService.UpdateQuestionRequest("Changed", "Changed", List.of())));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Question", question.getTitle());
        assertEquals("Body", question.getBody());
    }

    @Test
    void editingResolvedQuestionPreservesStatusAndAcceptedAnswer() {
        Member author = member("author");
        Member responder = member("responder");
        Question question = new Question("Question", "Body", author, new LinkedHashSet<>());
        Answer answer = new Answer(question, responder, "Answer");
        question.addAnswer(answer);
        question.accept(answer);
        when(questions.findById(question.getId())).thenReturn(Optional.of(question));

        QuestionService.QuestionDetail result = service.updateQuestion(
                "author",
                question.getId(),
                new QuestionService.UpdateQuestionRequest("Updated", "Updated body", List.of("java")));

        assertEquals(QuestionStatus.RESOLVED, result.status());
        assertEquals(answer.getId(), result.acceptedAnswerId());
        assertEquals(1, result.answers().size());
        assertTrue(result.answers().get(0).accepted());
    }

    @Test
    void rejectsInvalidTagsWithoutChangingQuestion() {
        Member author = member("author");
        Question question = new Question("Question", "Body", author, new LinkedHashSet<>());
        when(questions.findById(question.getId())).thenReturn(Optional.of(question));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateQuestion(
                        "author",
                        question.getId(),
                        new QuestionService.UpdateQuestionRequest("Changed", "Changed", List.of("invalid tag"))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Question", question.getTitle());
        assertEquals("Body", question.getBody());
    }

    @Test
    void editingMissingQuestionReturnsNotFound() {
        UUID questionId = UUID.randomUUID();
        when(questions.findById(questionId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateQuestion(
                        "author",
                        questionId,
                        new QuestionService.UpdateQuestionRequest("Changed", "Changed", List.of())));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void questionAuthorCanAcceptAnswerAndResolveQuestion() {
        Member author = member("author");
        Member responder = member("responder");
        Question question = new Question("Question", "Body", author, new LinkedHashSet<>());
        Answer answer = new Answer(question, responder, "Answer");
        question.addAnswer(answer);
        when(questions.findById(question.getId())).thenReturn(Optional.of(question));
        when(answers.findByIdAndQuestionId(answer.getId(), question.getId())).thenReturn(Optional.of(answer));

        QuestionService.QuestionDetail result = service.acceptAnswer("author", question.getId(), answer.getId());

        assertEquals(QuestionStatus.RESOLVED, result.status());
        assertEquals(answer.getId(), result.acceptedAnswerId());
        verify(questions).findById(question.getId());
    }

    private Member member(String username) {
        return new Member(username, username + "@example.test", username, "encoded");
    }
}
