package jp.co.sss.pr.test_function;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.AnswerForm;
import jp.co.sss.pr.form.AnswerListForm;
import jp.co.sss.pr.form.QuestionForm;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.QuestionRepository;




@Controller
public class TestController {

	@Autowired
	QuestionRepository quesRpstry;


	@Autowired
	AnswerRepository ansRpstry;

	@Autowired
	HttpSession session;

	//カテゴリー別問題選択画面
	@RequestMapping(path = "/test/category", method = RequestMethod.GET)
	public String selectCategory(Model model) {

		session.removeAttribute("question");
		session.removeAttribute("ans");
		session.removeAttribute("testFlag");

		QuestionForm questionForm = new QuestionForm();

		model.addAttribute(questionForm);

		return "test/select_category";
	}

	//カテゴリ別テスト解答画面
	@RequestMapping(path = "/test/category/start", method = RequestMethod.GET)
	public String startCategory(Model model,QuestionForm questionForm) {

		List<Question> quesList = quesRpstry.findAllByCategoryAndDifficultyLessThanEqualAndQuestionDelete(questionForm.getCategory(),questionForm.getDifficulty(),0);

		List<AnswerForm> ansList = new ArrayList<>();


		AnswerListForm ansForm = new AnswerListForm();

		for(int i=0;i<=quesList.size();i++) {

			AnswerForm af = new AnswerForm();

			af.setAnsOption(0);
			af.setCategory(0);

			ansList.add(af);

		}

		model.addAttribute("answerListForm",ansForm);
		session.setAttribute("ans", ansList);
		session.setAttribute("question",quesList);
		
		session.setAttribute("testFlag",1);

		return "test/show_test";
	}


	//苦手範囲特定問題選択画面
	@RequestMapping(path = "/test/diagnostic", method = RequestMethod.GET)
	public String selectDiagnostic(Model model) {

		session.removeAttribute("question");
		session.removeAttribute("ans");
		session.removeAttribute("testFlag");

		List<QuestionForm>qForm = new ArrayList<>();
		Integer difficulty=0;

		model.addAttribute(difficulty);
		model.addAttribute("question",qForm);


		return "test/select_diagnostic";
	}

	//苦手範囲特定テスト解答画面
	@RequestMapping(path = "/test/diagnostic/start", method = RequestMethod.GET)
	public String startDiagnostic(@RequestParam(value="category",required=false)List<Integer>catList, Integer difficulty, Model model) {

		if(catList==null || catList.size()<=2) {
			
			model.addAttribute("errorMessage","カテゴリを3つ以上選択してください");
			return "test/select_diagnostic";
		}


		List<Question> quesList = quesRpstry.findAllByCategoryInAndDifficultyLessThanEqualAndQuestionDelete(catList, difficulty,0);


		List<AnswerForm> ansList = new ArrayList<>();

		AnswerListForm ansForm = new AnswerListForm();

		for(int i=0;i<quesList.size();i++) {

			AnswerForm af = new AnswerForm();

			af.setAnsOption(0);

			ansList.add(af);

		}

		model.addAttribute("answerListForm",ansForm);
		session.setAttribute("ans", ansList);
		session.setAttribute("question",quesList);

		return "test/show_test";
	}



	//テスト解答確認画面
	@RequestMapping(path = "/test/check", method = RequestMethod.POST)
	public String checkTest(@Valid @ModelAttribute AnswerListForm answerForm  ,BindingResult result,Model model) {
		
		session.setAttribute("ans", answerForm.getResults());
		
		if(result.hasErrors()) {
			
			return "test/show_test";
		}

		return "test/review_test";
	}

	//テスト解答画面に戻る
	@RequestMapping(path = "/test/back", method = RequestMethod.POST)
	public String backTest(@ModelAttribute AnswerListForm ans ) {
		
		return "test/show_test";
	}

	//解答結果確認画面
	@RequestMapping(path = "/test/result", method = RequestMethod.POST)
	public String resulttTest(Model model,AnswerListForm ans) {

		CustomerBean user = new CustomerBean();
		Customer user1 = new Customer();
		List<AnswerForm> results = (List<AnswerForm>)session.getAttribute("ans");
		user = (CustomerBean)session.getAttribute("user");

		user1.setUserId(user.getUserId());

		int qCount = results.size();
		int caCount = 0;
		int[] cateCount = {0,0,0,0};
		int[] categCount = {0,0,0,0};


		for(AnswerForm aForm :results) {

			Answer answer = new Answer();
			Question que = new Question();

			answer.setCustomer(user1);

			que.setQuestionId(aForm.getQuestionId());
			answer.setQuestion(que);

			answer.setAnsOption(aForm.getAnsOption());

			answer.setCorrectOption(aForm.getCorrectOption());

			answer = ansRpstry.save(answer);

				switch(aForm.getCategory()) {
				
				case 1: 
					cateCount[0]++;
					if(aForm.getAnsOption() == aForm.getCorrectOption()) {caCount++;categCount[0]++;}
					break;
				case 2:
					cateCount[1]++;
					if(aForm.getAnsOption() == aForm.getCorrectOption()) {caCount++;categCount[1]++;}
					break;
				case 3:
					cateCount[2]++;
					if(aForm.getAnsOption() == aForm.getCorrectOption()) {caCount++;categCount[2]++;}
					break;
				case 4:
					cateCount[3]++;
					if(aForm.getAnsOption() == aForm.getCorrectOption()) {caCount++;categCount[3]++;}
					break;

				}
			
		}

		model.addAttribute("cateCorrect",categCount);
		model.addAttribute("cateCount",cateCount);
		model.addAttribute("qc",qCount);
		model.addAttribute("ca",caCount); 
		model.addAttribute("ans",results);

		session.removeAttribute("question");
		session.removeAttribute("ans");


		return "test/result_test";
	}

}
