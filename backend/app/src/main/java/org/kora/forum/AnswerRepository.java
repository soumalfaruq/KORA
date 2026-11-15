package org.kora.forum;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {
    Optional<Answer> findByIdAndQuestionId(UUID id, UUID questionId);
}
