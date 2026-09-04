package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.entity.QuestionOption;

public interface QuestionOptionReository extends JpaRepository<QuestionOption,Integer>{
	
	List<QuestionOption>findAllByQuestion(Question question);

}
