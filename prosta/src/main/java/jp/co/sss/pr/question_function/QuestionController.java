package jp.co.sss.pr.question_function;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.QuestionOptionRepository;
import jp.co.sss.pr.repository.QuestionRepository;



@Controller
public class QuestionController {
	@Autowired
	QuestionRepository qRepository;
	@Autowired
	QuestionOptionRepository oRepository;
	@Autowired
	HttpSession session;
	
	private List<Integer> getFlag(){
		
		CustomerBean ctBean = (CustomerBean)session.getAttribute("user");
		List<Integer> delFlag = new ArrayList<>(List.of(0));

		if((ctBean.getPermission()!=null) && (ctBean.getPermission()==2)) delFlag.add(1);
		
		return delFlag;
	}


	@RequestMapping(path = "/question")
	public String questionFindAll(Model model) {
		List<Question>QuestionList=qRepository.findAllByQuestionDeleteInOrderByDifficultyAscCategoryAsc(getFlag());
		model.addAttribute("Questions",QuestionList);
		model.addAttribute("QuestionResult","全件検索");
		return "question/question_list";
	}

	@RequestMapping(path = "/question/list/category")
	public String questionList(Model model,Integer category) {

		String resultText = "";
		if(category==1) {
			resultText = "Java"; 
		}else if(category==2) {
			resultText = "Spring"; 
		}else if(category==3) {
			resultText = "HTML"; 
		}else {
			resultText = "SQL"; }

		model.addAttribute("QuestionResult",resultText);
		List<Question> categoryList = qRepository.findByCategoryAndQuestionDeleteInOrderByDifficultyAscCategoryAsc(category,getFlag());
		model.addAttribute("Questions",categoryList);

		return "question/question_list";}

	@RequestMapping(path = "/question/list/text")
	public String questionText(Model model,String questionText) {
		
		List<Question> TextList = qRepository.findByQuestionTextContainingAndQuestionDeleteInOrderByDifficultyAscCategoryAsc(questionText,getFlag());
		model.addAttribute("Questions",TextList);
		
		if(questionText==null || questionText.isEmpty()) {
			model.addAttribute("QuestionResult","全件検索");
		}else {
			model.addAttribute("QuestionResult",questionText);
		}

		return "question/question_list";}

	@RequestMapping(path = "/question/list/difficulty")
	public String questionResult(Model model,Integer difficulty) {

		String resultd = "";
		if (difficulty==1) {
			resultd = "★☆☆"; 
		}else if(difficulty==2) {
			resultd = "★★☆"; 
		}else  {
			resultd = "★★★"; 
		}

		model.addAttribute("QuestionResult",resultd);
		List<Question> difList = qRepository.findByDifficultyAndQuestionDeleteInOrderByCategory(difficulty,getFlag());
		model.addAttribute("Questions",difList);

		return "question/question_list";
	}
	
	@RequestMapping(path = "/question/list/delete")
	public String questionDelt(Model model,Integer questionDelete) {

		String resultd = "";
		if (questionDelete==0) {
			resultd = "表示"; 
		}else{
			resultd = "非表示"; 
		}
		model.addAttribute("QuestionResult",resultd);
		List<Question> difList = qRepository.findByQuestionDeleteOrderByDifficultyAscCategoryAsc(questionDelete);
		model.addAttribute("Questions",difList);

		return "question/question_list";
	}

}
//@RequestMapping(path = "/question/detail/{questionId}")
//public String questionShow(@PathVariable("questionId") Integer questionId, Model model) {
//	List<Question>QuestionList=qRepository.findAllByOrderByDifficulty();
		//	model.addAttribute("Questions",QuestionList);
		//	return "question/question_list";
		//}}
