package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Favorite;
import jp.co.sss.pr.entity.Question;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {

    List<Favorite> findByCustomer(Customer customer);

    @Query("select q from Question q where exists "
            + "(select f.favoriteId from Favorite f where f.question = q and f.customer.userId = :userId) "
            + "order by q.difficulty, q.questionId")
    List<Question> findBookmarkedQuestions(@Param("userId") Integer userId);

    boolean existsByCustomerAndQuestion(Customer customer, Question question);

    boolean existsByCustomerUserIdAndQuestionQuestionId(Integer userId, Integer questionId);

    boolean existsByQuestion(Question question);

    void deleteByCustomerAndQuestion(Customer customer, Question question);
}
