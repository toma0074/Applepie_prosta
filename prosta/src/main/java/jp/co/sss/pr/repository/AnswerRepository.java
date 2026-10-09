package jp.co.sss.pr.repository;

import java.util.List;

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

    List<Answer> findAllByQuestion(Question question);

    /**
     * 指定ユーザーの正解件数（correct_flag = true）を返す。
     */
    int countByCustomerAndCorrectFlagTrue(Customer customer);

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

