package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;


@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer>{
	
	List<Answer> findAllByCustomerOrderByAnsIdDesc(Customer customer);
	
	List<Answer> findAllByCustomerOrderByAnsIdAsc(Customer customer);

	List<Answer> findAllByQuestion(Question question);
}
