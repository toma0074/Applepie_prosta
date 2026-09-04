package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.sss.pr.entity.Question;


public interface QuestionRepository extends JpaRepository<Question,Integer>{
	
	List<Question>findAllByQuestionDeleteInOrderByDifficultyAscCategoryAsc(List<Integer> questionDelete);
	
	Question findByQuestionText(String questionText);
	
	List<Question> findByQuestionDeleteOrderByDifficultyAscCategoryAsc(Integer deleteFlag);

	public List<Question> findByCategoryAndQuestionDeleteInOrderByDifficultyAscCategoryAsc(Integer category,List<Integer> questionDelete);

	public List<Question> findByQuestionTextContainingAndQuestionDeleteInOrderByDifficultyAscCategoryAsc(String questionText,List<Integer> questionDelete);

	public List<Question> findByDifficultyAndQuestionDeleteInOrderByCategory(Integer difficulty,List<Integer> questionDelete);
	
	List<Question>findAllByCategoryAndDifficultyLessThanEqualAndQuestionDelete(Integer category, Integer difficulty,Integer questionDelete);
	
	List<Question>findAllByCategoryInAndDifficultyLessThanEqualAndQuestionDelete(List<Integer> list, Integer difficulty,Integer questionDelete);

	List<Question> findByQuestionId(Integer questionId);
	


	
}
