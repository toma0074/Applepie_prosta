package jp.co.sss.pr.repository;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {

    List<Answer> findAllByCustomerOrderByAnsIdDesc(Customer customer);

    List<Answer> findAllByCustomerOrderByAnsIdAsc(Customer customer);

    /**
     * 解答履歴の絞り込み検索（マイページ用）。
     * correct: null=全て / 1=正解のみ / 0=不正解のみ。keyword は LIKE パターン。
     * 並び順は Pageable の Sort で指定する。
     */
    @Query("""
            SELECT a FROM Answer a JOIN a.question q
            WHERE a.customer = :customer
              AND (:categoryId IS NULL OR q.category.categoryId = :categoryId)
              AND (:difficulty IS NULL OR q.difficulty = :difficulty)
              AND q.questionText LIKE :keyword
              AND (:correct IS NULL
                   OR (:correct = 1 AND a.correctFlag = true)
                   OR (:correct = 0 AND a.correctFlag = false))
              AND (:fromDate IS NULL OR a.ansDate >= :fromDate)
              AND (:toDate IS NULL OR a.ansDate <= :toDate)
            """)
    Page<Answer> searchHistory(@Param("customer") Customer customer,
            @Param("categoryId") Integer categoryId,
            @Param("difficulty") Integer difficulty,
            @Param("keyword") String keyword,
            @Param("correct") Integer correct,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    List<Answer> findAllByQuestion(Question question);

    boolean existsByQuestion(Question question);

    /** 全ユーザー分の問題別の解答回数・正解回数 */
    @Query("""
            SELECT a.question.questionId AS questionId,
                   COUNT(a.ansId) AS attemptCount,
                   COALESCE(SUM(CASE WHEN a.correctFlag = true THEN 1 ELSE 0 END), 0) AS correctCount
            FROM Answer a
            WHERE a.question.questionId IN :questionIds
            GROUP BY a.question.questionId
            """)
    List<QuestionAttemptStat> summarizeByQuestionIds(@Param("questionIds") List<Integer> questionIds);

    /**
     * 指定ユーザーの正解件数（correct_flag = true）を返す。
     */
    int countByCustomerAndCorrectFlagTrue(Customer customer);

    @Query("""
            SELECT c.userId AS userId,
                   COUNT(a.ansId) AS answerCount,
                   COALESCE(SUM(CASE WHEN a.correctFlag = true THEN 1 ELSE 0 END), 0) AS correctCount
            FROM Customer c
            LEFT JOIN Answer a ON a.customer = c
              AND (:categoryId IS NULL OR a.question.category.categoryId = :categoryId)
              AND (:fromDate IS NULL OR a.ansDate >= :fromDate)
              AND (:toDate IS NULL OR a.ansDate <= :toDate)
            WHERE c.userId IN :userIds
            GROUP BY c.userId
            """)
    List<FriendStudySummary> summarizeStudy(
            @Param("userIds") List<Integer> userIds,
            @Param("categoryId") Integer categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /** 指定ユーザーの問題別の解答回数・正解回数（解答回数が minAttempts 以上のもの）。 */
    @Query("""
            SELECT a.question.questionId AS questionId,
                   COUNT(a.ansId) AS attemptCount,
                   COALESCE(SUM(CASE WHEN a.correctFlag = true THEN 1 ELSE 0 END), 0) AS correctCount
            FROM Answer a
            WHERE a.customer = :customer
              AND a.question IS NOT NULL
            GROUP BY a.question.questionId
            HAVING COUNT(a.ansId) >= :minAttempts
            """)
    List<QuestionAttemptStat> summarizeByQuestion(
            @Param("customer") Customer customer,
            @Param("minAttempts") long minAttempts);

    /**
     * 指定ユーザーが「その日の全問正解」した日数を返す。
     *
     * <p>同一 ans_date において、その日の全解答が correct_flag=true の日をカウントする。
     * つまり「1日でも不正解がある日」はカウントしない。
     */
    @Query("""
            SELECT COUNT(DISTINCT a.ansDate)
            FROM Answer a
            WHERE a.customer = :customer
              AND a.ansDate IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM Answer a2
                  WHERE a2.customer = :customer
                    AND a2.ansDate = a.ansDate
                    AND a2.correctFlag = false
              )
            """)
    int countPerfectDaysByCustomer(@Param("customer") Customer customer);
}
