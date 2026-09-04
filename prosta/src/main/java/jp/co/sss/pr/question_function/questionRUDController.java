package jp.co.sss.pr.question_function;

import jakarta.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jp.co.sss.pr.bean.QuestionBean;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.entity.QuestionOption;
import jp.co.sss.pr.form.QuestionForm;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.QuestionOptionRepository;
import jp.co.sss.pr.repository.QuestionRepository;

@Controller
public class questionRUDController {
	
	@Autowired
	QuestionRepository qRepository;
	
	@Autowired
	QuestionOptionRepository oRepository;
	
	@Autowired
	AnswerRepository aRepository;
	
	//登録入力画面遷移
	@RequestMapping(path = "/question/regist/input")
	public String questionRegist(Model model) {
		
		QuestionForm qForm = new QuestionForm();
		
		model.addAttribute("question",qForm);
		
		return "question/question_update";
		
	}
	//更新確認画面遷移
		@RequestMapping(path = "/question/update/check",method=RequestMethod.POST)
		public String questionUpdateCheck(@Valid @ModelAttribute("question") QuestionForm questionForm,BindingResult result,Model model) {
			if(result.hasErrors()) {
				return "question/question_update";
			}else {
			model.addAttribute("question",questionForm);
			model.addAttribute("mode", "create");
			return "question/question_check";
			}
		}
	//登録入力確認画面遷移
	//@RequestMapping(path = "/question/regist/check")
	//public String questionRegistCheck(@ModelAttribute("question") QuestionForm questionForm,Model model) {
		//model.addAttribute("question",questionForm);
	//	model.addAttribute("mode", "create");
	//	return "question/question_check";
	//}
	//登録完了画面遷移
	@RequestMapping(path = "/question/regist/complete",method=RequestMethod.POST)
	public String questionRegistComplete(QuestionForm questionForm, Model model) {
		Question question = new Question();
		BeanUtils.copyProperties(questionForm, question,"questionId");
		question=qRepository.save(question);
	    
	    //question = qRepository.findByQuestionText(questionForm.getQuestionText());
	    
	    //questionForm.setQuestionId(question.getQuestionId());
	    
	    QuestionOption option = new QuestionOption();
	    BeanUtils.copyProperties(questionForm, option,"questionId");

	    option.setQuestion(question);

	    oRepository.save(option);
	    model.addAttribute("mode", "create");
		return "question/question_complete";
	}
	
	
	//削除確認画面
	@RequestMapping(path = "/question/delrest/check",method=RequestMethod.POST)
	public String questionCheck(@RequestParam Integer questionId,@RequestParam String mode ,Model model) {
		Question q = qRepository.getReferenceById(questionId);
		QuestionBean qBean = new QuestionBean();
		BeanUtils.copyProperties(q,qBean);
		//qFormにすでにある問題の情報をコピーする
		
		  QuestionOption option = oRepository.getReferenceById(questionId);
	     qBean.setOptionA(option.getOptionA());
	     qBean.setOptionB(option.getOptionB());
	     qBean.setOptionC(option.getOptionC());
		model.addAttribute("question",qBean);
		model.addAttribute("mode", mode);
		return "question/question_check";
	}
	
	//復元/削除完了画面遷移
	@Transactional
	@RequestMapping(path = "/question/delrest/complete",method=RequestMethod.POST)
	public String deleteComplete(@RequestParam Integer questionId ,@RequestParam String mode,Model model) {
//		Question question = new Question();
//		List<Answer> aList=aRepository.findAllByQuestion(question);
//		
//		for(Answer delAns: aList){
//			
//			aRepository.deleteById(delAns.getAnsId());
//		
//		}
//		oRepository.deleteById(questionId);
//		qRepository.deleteById(questionId);
		
		Question question = qRepository.getReferenceById(questionId);
		if(mode.equals("delete"))question.setQuestionDelete(1);
		if(mode.equals("restore"))question.setQuestionDelete(0);
		qRepository.save(question);
		
		model.addAttribute("mode",mode);
		return "question/question_complete";
	}

	
	//更新画面遷移
	@RequestMapping(path = "/question/update/input",method=RequestMethod.POST)
	public String questionUpdate(@RequestParam Integer questionId ,Model model) {
		Question q = qRepository.getReferenceById(questionId);
		QuestionBean qBean = new QuestionBean();
		BeanUtils.copyProperties(q,qBean);
		//qFormにすでにある問題の情報をコピーする
		
		  QuestionOption option = oRepository.getReferenceById(questionId);
	     qBean.setOptionA(option.getOptionA());
	     qBean.setOptionB(option.getOptionB());
	     qBean.setOptionC(option.getOptionC());
		model.addAttribute("question",qBean);
		model.addAttribute("mode", "create");
		return "question/question_update";
		
	}


}
