package org.kora.forum;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.kora.auth.Member;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    private UUID id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestionStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<Answer> answers = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_answer_id")
    private Answer acceptedAnswer;

    @ElementCollection
    @CollectionTable(name = "question_tags", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "tag", nullable = false, length = 40)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Question() {}

    public Question(String title, String body, Member author, Set<String> tags) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.body = body;
        this.author = author;
        this.status = QuestionStatus.OPEN;
        this.tags = new LinkedHashSet<>(tags);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void accept(Answer answer) {
        if (status == QuestionStatus.CLOSED || !answer.getQuestion().getId().equals(id)) {
            throw new IllegalArgumentException("Answer cannot be accepted for this question");
        }
        acceptedAnswer = answer;
        status = QuestionStatus.RESOLVED;
        updatedAt = Instant.now();
    }

    public void edit(String title, String body, Set<String> tags) {
        if (!this.title.equals(title) || !this.body.equals(body) || !this.tags.equals(tags)) {
            this.title = title;
            this.body = body;
            this.tags.clear();
            this.tags.addAll(tags);
            updatedAt = Instant.now();
        }
    }

    public void addAnswer(Answer answer) {
        if (status != QuestionStatus.OPEN) {
            throw new IllegalStateException("Only open questions can receive answers");
        }
        answers.add(answer);
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public Member getAuthor() {
        return author;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public Answer getAcceptedAnswer() {
        return acceptedAnswer;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
