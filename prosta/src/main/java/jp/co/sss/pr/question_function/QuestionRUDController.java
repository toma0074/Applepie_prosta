package jp.co.sss.pr.question_function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.QuestionForm;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.QuestionRepository;

@Controller
public class QuestionRUDController {

    @Autowired
    QuestionRepository qRepository;

    @Autowired
    CategoryRepository categoryRepository;

    // 登録入力画面
    @RequestMapping(path = "/question/regist/input")
    public String questionRegist(Model model) {
        model.addAttribute("question", new QuestionForm());
        return "question/question_update";
    }
    
	// 更新確認画面
    @RequestMapping(path = "/question/update/check", method = RequestMethod.POST)
    public String questionUpdateCheck(@Valid @ModelAttribute("question") QuestionForm questionForm,
                                      BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "question/question_update";
        }
        model.addAttribute("question", questionForm);
        model.addAttribute("mode", "create");
        return "question/question_check";
    }
    
		// 登録完了
	    @RequestMapping(path = "/question/regist/complete", method = RequestMethod.POST)
	    public String questionRegistComplete(QuestionForm questionForm, Model model) {
	        Question question = new Question();

	        // category ID → Category entity に変換
	        if (questionForm.getCategory() != null) {
	            question.setCategory(categoryRepository.getReferenceById(questionForm.getCategory()));
	        }
	        question.setDifficulty(questionForm.getDifficulty());
	        question.setQuestionText(questionForm.getQuestionText());
	        question.setOptionA(questionForm.getOptionA());
	        question.setOptionB(questionForm.getOptionB());
	        question.setOptionC(questionForm.getOptionC());
	        question.setCorrectOption(questionForm.getCorrectOption());
	        qRepository.save(question);

	        model.addAttribute("mode", "create");
	        return "question/question_complete";
	    }
	
	// 削除/復元確認画面
    @RequestMapping(path = "/question/delrest/check", method = RequestMethod.POST)
    public String questionCheck(@RequestParam Integer questionId, @RequestParam String mode, Model model) {
        Question q = qRepository.getReferenceById(questionId);
        QuestionForm qForm = new QuestionForm();
        qForm.setQuestionId(q.getQuestionId());
        if (q.getCategory() != null) qForm.setCategory(q.getCategory().getCategoryId());
        qForm.setDifficulty(q.getDifficulty());
        qForm.setQuestionText(q.getQuestionText());
        qForm.setOptionA(q.getOptionA());
        qForm.setOptionB(q.getOptionB());
        qForm.setOptionC(q.getOptionC());
        qForm.setCorrectOption(q.getCorrectOption());
        model.addAttribute("question", qForm);
        model.addAttribute("mode", mode);
        return "question/question_check";
    }

	
	// 削除/復元完了
    @Transactional
    @RequestMapping(path = "/question/delrest/complete", method = RequestMethod.POST)
    public String deleteComplete(@RequestParam Integer questionId, @RequestParam String mode, Model model) {
        Question question = qRepository.getReferenceById(questionId);
        qRepository.save(question);
        model.addAttribute("mode", mode);
        return "question/question_complete";
    }

	
	// 更新入力画面
    @RequestMapping(path = "/question/update/input", method = RequestMethod.POST)
    public String questionUpdate(@RequestParam Integer questionId, Model model) {
        Question q = qRepository.getReferenceById(questionId);
        QuestionForm qForm = new QuestionForm();
        qForm.setQuestionId(q.getQuestionId());
        if (q.getCategory() != null) qForm.setCategory(q.getCategory().getCategoryId());
        qForm.setDifficulty(q.getDifficulty());
        qForm.setQuestionText(q.getQuestionText());
        qForm.setOptionA(q.getOptionA());
        qForm.setOptionB(q.getOptionB());
        qForm.setOptionC(q.getOptionC());
        qForm.setCorrectOption(q.getCorrectOption());
        model.addAttribute("question", qForm);
        model.addAttribute("mode", "create");
        return "question/question_update";
    }


}
