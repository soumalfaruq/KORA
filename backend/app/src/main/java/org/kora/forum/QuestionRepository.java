package org.kora.forum;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    @Query("""
            select distinct q from Question q left join q.tags tag
            where (:term = '' or lower(q.title) like lower(concat('%', :term, '%'))
                or lower(q.body) like lower(concat('%', :term, '%'))
                or lower(tag) like lower(concat('%', :term, '%')))
              and (:status is null or q.status = :status)
              and (:unanswered = false or q.answers is empty)
            """)
    Page<Question> search(
            @Param("term") String term,
            @Param("status") QuestionStatus status,
            @Param("unanswered") boolean unanswered,
            Pageable pageable);
}
