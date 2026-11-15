package org.kora.forum;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.kora.auth.Member;
import org.kora.auth.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestionService {
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-z0-9+#.-]{1,40}$");
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final MemberRepository members;

    public QuestionService(
            QuestionRepository questions,
            AnswerRepository answers,
            MemberRepository members) {
        this.questions = questions;
        this.answers = answers;
        this.members = members;
    }

    @Transactional(readOnly = true)
    public Page<QuestionSummary> search(String query, String status, boolean unanswered, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values");
        }
        QuestionStatus parsedStatus = parseStatus(status);
        String term = query == null ? "" : query.trim();
        if (term.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query is too long");
        }
        return questions.search(
                        term,
                        parsedStatus,
                        unanswered,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(QuestionSummary::from);
    }

    @Transactional(readOnly = true)
    public QuestionDetail get(UUID id) {
        Question question = findQuestion(id);
        List<AnswerView> answerViews = question.getAnswers().stream().map(AnswerView::from).toList();
        return QuestionDetail.from(question, answerViews);
    }

    @Transactional
    public QuestionDetail create(String username, CreateQuestionRequest request) {
        Member author = findMember(username);
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String tag : request.tags()) {
            String normalized = tag.trim().toLowerCase(Locale.ROOT);
            if (!TAG_PATTERN.matcher(normalized).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tags may contain letters, numbers, +, #, . and -");
            }
            tags.add(normalized);
        }
        if (tags.size() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A question can have at most 8 tags");
        }
        Question question = questions.save(new Question(
                request.title().trim(),
                request.body().trim(),
                author,
                tags));
        return QuestionDetail.from(question, List.of());
    }

    @Transactional
    public AnswerView answer(String username, UUID questionId, AnswerRequest request) {
        Question question = findQuestion(questionId);
        if (question.getStatus() != QuestionStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This question is no longer open");
        }
        Answer answer = answers.save(new Answer(question, findMember(username), request.body().trim()));
        question.addAnswer(answer);
        return AnswerView.from(answer);
    }

    @Transactional
    public QuestionDetail acceptAnswer(String username, UUID questionId, UUID answerId) {
        Question question = findQuestion(questionId);
        if (!question.getAuthor().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the question author can accept a solution");
        }
        Answer answer = answers.findByIdAndQuestionId(answerId, questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));
        try {
            question.accept(answer);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return QuestionDetail.from(question, question.getAnswers().stream().map(AnswerView::from).toList());
    }

    private Question findQuestion(UUID id) {
        return questions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    private Member findMember(String username) {
        return members.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private QuestionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return QuestionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown question status");
        }
    }

    public record CreateQuestionRequest(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 180) String title,
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 20000) String body,
            @jakarta.validation.constraints.NotNull
            @jakarta.validation.constraints.Size(max = 8) List<@jakarta.validation.constraints.NotBlank String> tags) {}

    public record AnswerRequest(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 20000) String body) {}

    public record QuestionSummary(
            UUID id,
            String title,
            String body,
            QuestionStatus status,
            String author,
            List<String> tags,
            int answerCount,
            java.time.Instant createdAt) {
        static QuestionSummary from(Question question) {
            return new QuestionSummary(
                    question.getId(),
                    question.getTitle(),
                    question.getBody(),
                    question.getStatus(),
                    question.getAuthor().getDisplayName(),
                    List.copyOf(question.getTags()),
                    question.getAnswers().size(),
                    question.getCreatedAt());
        }
    }

    public record AnswerView(UUID id, String body, String author, java.time.Instant createdAt, boolean accepted) {
        static AnswerView from(Answer answer) {
            return new AnswerView(
                    answer.getId(),
                    answer.getBody(),
                    answer.getAuthor().getDisplayName(),
                    answer.getCreatedAt(),
                    answer.getQuestion().getAcceptedAnswer() != null
                            && answer.getQuestion().getAcceptedAnswer().getId().equals(answer.getId()));
        }
    }

    public record QuestionDetail(
            UUID id,
            String title,
            String body,
            QuestionStatus status,
            String author,
            String authorUsername,
            List<String> tags,
            java.time.Instant createdAt,
            UUID acceptedAnswerId,
            List<AnswerView> answers) {
        static QuestionDetail from(Question question, List<AnswerView> answers) {
            return new QuestionDetail(
                    question.getId(),
                    question.getTitle(),
                    question.getBody(),
                    question.getStatus(),
                    question.getAuthor().getDisplayName(),
                    question.getAuthor().getUsername(),
                    List.copyOf(question.getTags()),
                    question.getCreatedAt(),
                    question.getAcceptedAnswer() == null ? null : question.getAcceptedAnswer().getId(),
                    answers);
        }
    }
}
