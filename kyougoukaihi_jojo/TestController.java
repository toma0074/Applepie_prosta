package jp.co.sss.pr.test_function;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.AnswerForm;
import jp.co.sss.pr.form.AnswerListForm;
import jp.co.sss.pr.form.QuestionForm;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.service.LevelService;

@Controller
public class TestController {

<<<<<<< HEAD
    @Autowired
    QuestionRepository quesRpstry;

    @Autowired
    AnswerRepository ansRpstry;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    LevelService levelService;

    @Autowired
    HttpSession session;

    // カテゴリー別問題選択画面
    @RequestMapping(path = "/test/category", method = RequestMethod.GET)
    public String selectCategory(Model model) {
        session.removeAttribute("question");
        session.removeAttribute("ans");
        session.removeAttribute("testFlag");

        // カテゴリ一覧を表示用に渡す
        model.addAttribute("categoryList", categoryRepository.findAll());
        model.addAttribute("questionForm", new QuestionForm());
        return "test/select_category";
    }

    // カテゴリ別テスト解答画面
    @RequestMapping(path = "/test/category/start", method = RequestMethod.GET)
    public String startCategory(Model model, QuestionForm questionForm) {
        Category category = categoryRepository.getReferenceById(questionForm.getCategory());
        List<Question> quesList = quesRpstry
                .findAllByCategoryAndDifficultyLessThanEqual(
                        category, questionForm.getDifficulty());

        List<AnswerForm> ansList = buildAnswerFormList(quesList.size());

        model.addAttribute("answerListForm", new AnswerListForm());
        session.setAttribute("ans", ansList);
        session.setAttribute("question", quesList);
        session.setAttribute("testFlag", 1);

        return "test/show_test";
    }

    // 苦手範囲特定問題選択画面
    @RequestMapping(path = "/test/diagnostic", method = RequestMethod.GET)
    public String selectDiagnostic(Model model) {
        session.removeAttribute("question");
        session.removeAttribute("ans");
        session.removeAttribute("testFlag");

        model.addAttribute("categoryList", categoryRepository.findAll());
        model.addAttribute("questionForm", new QuestionForm());
        return "test/select_diagnostic";
    }

    // 苦手範囲特定テスト解答画面
    @RequestMapping(path = "/test/diagnostic/start", method = RequestMethod.GET)
    public String startDiagnostic(
            @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds,
            Integer difficulty, Model model) {

        if (categoryIds == null || categoryIds.size() <= 2) {
            model.addAttribute("errorMessage", "カテゴリを3つ以上選択してください");
            model.addAttribute("categoryList", categoryRepository.findAll());
            return "test/select_diagnostic";
        }

        // Integer の ID リスト → Category エンティティリストに変換
        List<Category> categories = categoryRepository.findAllById(categoryIds);

        List<Question> quesList = quesRpstry
                .findAllByCategoryInAndDifficultyLessThanEqual(
                        categories, difficulty);

        List<AnswerForm> ansList = buildAnswerFormList(quesList.size());

        model.addAttribute("answerListForm", new AnswerListForm());
        session.setAttribute("ans", ansList);
        session.setAttribute("question", quesList);

        return "test/show_test";
    }

    // テスト解答確認画面
    @RequestMapping(path = "/test/check", method = RequestMethod.POST)
    public String checkTest(@Valid @ModelAttribute AnswerListForm answerForm,
                            BindingResult result, Model model) {
        session.setAttribute("ans", answerForm.getResults());

        if (result.hasErrors()) {
            return "test/show_test";
        }
        return "test/review_test";
    }

    // テスト解答画面に戻る
    @RequestMapping(path = "/test/back", method = RequestMethod.POST)
    public String backTest(@ModelAttribute AnswerListForm ans) {
        return "test/show_test";
    }

    // 解答結果確認画面
    @RequestMapping(path = "/test/result", method = RequestMethod.POST)
    public String resulttTest(Model model, AnswerListForm ans) {

        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        Customer user = customerRepository.getReferenceById(userBean.getUserId());

        @SuppressWarnings("unchecked")
        List<AnswerForm> results = (List<AnswerForm>) session.getAttribute("ans");

        int qCount  = results.size();
        int caCount = 0;
        int[] cateCount  = {0, 0, 0, 0};
        int[] categCount = {0, 0, 0, 0};
        LocalDate today = LocalDate.now();

        for (AnswerForm aForm : results) {
            boolean isCorrect = aForm.getAnsOption() != null
                    && aForm.getAnsOption().equals(aForm.getCorrectOption());

            Answer answer = new Answer();
            Question que = new Question();
            que.setQuestionId(aForm.getQuestionId());

            answer.setCustomer(user);
            answer.setQuestion(que);
            answer.setAnsOption(aForm.getAnsOption());
            answer.setCorrectOption(aForm.getCorrectOption());
            answer.setCorrectFlag(isCorrect);   // ← correct_flag に保存
            answer.setAnsDate(today);            // ← ans_date に保存
            ansRpstry.save(answer);

            if (isCorrect) caCount++;

            // カテゴリ別集計（category_id が 1〜4 の場合）
            Integer catId = aForm.getCategory();
            if (catId != null && catId >= 1 && catId <= 4) {
                cateCount[catId - 1]++;
                if (isCorrect) categCount[catId - 1]++;
            }
        }

        // ---- レベル再計算・実績チェック ----
        List<Achievement> newAchievements = levelService.processTestResult(user, caCount, qCount);

        if (!newAchievements.isEmpty()) {
            session.setAttribute("newAchievements", newAchievements);
        }

        // 最新レベルを取得（processTestResult 内で save 済み）
        Customer updatedUser = customerRepository.getReferenceById(user.getUserId());
        int currentLevel = updatedUser.getUserLevel();
        boolean isPerfect = qCount > 0 && caCount == qCount;

        model.addAttribute("cateCorrect",    categCount);
        model.addAttribute("cateCount",      cateCount);
        model.addAttribute("qc",             qCount);
        model.addAttribute("ca",             caCount);
        model.addAttribute("ans",            results);
        model.addAttribute("currentLevel",   currentLevel);
        model.addAttribute("loginCount",     updatedUser.getLoginCount());
        model.addAttribute("isPerfect",      isPerfect);
        model.addAttribute("newAchievements", newAchievements);

        session.removeAttribute("question");
        session.removeAttribute("ans");

        return "test/result_test";
    }

    // ---- ユーティリティ ----
    private List<AnswerForm> buildAnswerFormList(int size) {
        List<AnswerForm> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            AnswerForm af = new AnswerForm();
            af.setAnsOption(0);
            list.add(af);
        }
        return list;
    }
}
=======
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
	
//	【追加開始】2026/10/04
	
	//苦手克服問題選択画面
	@RequestMapping(path = "/test/overcoming", method = RequestMethod.GET)
	public String selectOvercoming(Model model) {

		session.removeAttribute("question");
		session.removeAttribute("ans");
		session.removeAttribute("testFlag");

		List<QuestionForm>qForm = new ArrayList<>();
		Integer difficulty=0;

		model.addAttribute(difficulty);
		model.addAttribute("question",qForm);


		return "test/select_overcoming";
	}

	//苦手克服テスト解答画面
	@RequestMapping(path = "/test/overcoming/start", method = RequestMethod.GET)
	public String startOvercoming(@RequestParam(value="category",required=false)List<Integer>catList, Integer difficulty, Model model) {
		
		if(catList==null || catList.size()==0) {
			
			model.addAttribute("errorMessage","カテゴリを1つ以上選択してください");
			return "test/select_overcoming";
		}

		//検索条件を解答回数と正解回数で検索に変更（まだできてない）例：解答回数と正解回数で正答率が70％以下のものを抽出など
		List<Question> quesList = quesRpstry.findAllByCategoryInAndDifficultyLessThanEqualAndQuestionDelete(catList, difficulty,0);
		
		
		if(quesList==null || quesList.size() == 0) {
			
			model.addAttribute("errorMessage","解答できる問題がありません");
			return "test/select_overcoming";
		}


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
	
//	【追加終了】2026/10/04



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
>>>>>>> dbbeeff4a6dc45b31000f5bdad3215498d51e015
