package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.QuestionReport;
import jp.co.sss.pr.entity.QuestionReportReason;
import jp.co.sss.pr.entity.QuestionReportStatus;

@Repository
public interface QuestionReportRepository extends JpaRepository<QuestionReport, Integer> {

    @EntityGraph(attributePaths = { "customer", "question" })
    Page<QuestionReport> findByStatus(QuestionReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "customer", "question" })
    Page<QuestionReport> findAllBy(Pageable pageable);

    long countByStatus(QuestionReportStatus status);

    boolean existsByCustomerUserIdAndQuestionQuestionIdAndReasonAndStatus(
            Integer userId, Integer questionId, QuestionReportReason reason, QuestionReportStatus status);

    List<QuestionReport> findTop5ByCustomerUserIdAndQuestionQuestionIdOrderByCreatedAtDescReportIdDesc(
            Integer userId, Integer questionId);
}