package jp.co.sss.pr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.QuestionNote;

@Repository
public interface QuestionNoteRepository extends JpaRepository<QuestionNote, Integer> {

    Optional<QuestionNote> findByCustomerUserIdAndQuestionQuestionId(Integer userId, Integer questionId);

    long deleteByCustomerUserIdAndQuestionQuestionId(Integer userId, Integer questionId);
}