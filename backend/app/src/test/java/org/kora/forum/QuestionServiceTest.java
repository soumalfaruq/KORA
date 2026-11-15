package org.kora.forum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Optional;
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
