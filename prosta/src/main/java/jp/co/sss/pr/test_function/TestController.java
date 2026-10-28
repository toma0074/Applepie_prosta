package jp.co.sss.pr.test_function;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import jp.co.sss.pr.service.WeakQuestionService;

@Controller
public class TestController {

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
    WeakQuestionService weakQuestionService;

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
    public String startCategory(Model model, QuestionForm questionForm,
            @RequestParam(defaultValue = "false") boolean timer) {
        if (questionForm.getCategory() == null || questionForm.getDifficulty() == null) {
            return "redirect:/test/category";
        }
        Category category = categoryRepository.getReferenceById(questionForm.getCategory());
        List<Question> quesList = pickRandom(quesRpstry
                .findAllByCategoryAndDifficulty(
                        category, questionForm.getDifficulty()));

        List<AnswerForm> ansList = buildAnswerFormList(quesList.size());

        model.addAttribute("answerListForm", new AnswerListForm());
        session.setAttribute("ans", ansList);
        session.setAttribute("question", quesList);
        session.setAttribute("testFlag", 1);
        startTimer(quesList.size(), timer);
        addTimer(model);

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
            Integer difficulty, Model model,
            @RequestParam(defaultValue = "false") boolean timer) {

        if (categoryIds == null || categoryIds.size() <= 2) {
            model.addAttribute("errorMessage", "カテゴリを3つ以上選択してください");
            model.addAttribute("categoryList", categoryRepository.findAll());
            return "test/select_diagnostic";
        }

        // Integer の ID リスト → Category エンティティリストに変換
        List<Category> categories = categoryRepository.findAllById(categoryIds);

        List<Question> quesList = pickBalanced(quesRpstry
                .findAllByCategoryInAndDifficulty(
                        categories, difficulty));

        List<AnswerForm> ansList = buildAnswerFormList(quesList.size());

        model.addAttribute("answerListForm", new AnswerListForm());
        session.setAttribute("ans", ansList);
        session.setAttribute("question", quesList);
        startTimer(quesList.size(), timer);
        addTimer(model);

        return "test/show_test";
    }

    // 弱点問題（正答率の低い問題）の一覧画面
    @RequestMapping(path = "/test/weak", method = RequestMethod.GET)
    public String selectWeak(Model model) {
        session.removeAttribute("question");
        session.removeAttribute("ans");
        session.removeAttribute("testFlag");

        model.addAttribute("weakList", weakQuestionService.pick(currentUser(), WeakQuestionService.MAX_QUESTIONS));
        model.addAttribute("minAttempts", WeakQuestionService.MIN_ATTEMPTS);
        return "test/select_weak";
    }

    // 弱点問題テスト解答画面
    @RequestMapping(path = "/test/weak/start", method = RequestMethod.GET)
    public String startWeak(Model model,
            @RequestParam(defaultValue = "false") boolean timer) {
        List<WeakQuestionService.WeakQuestion> weakList =
                weakQuestionService.pick(currentUser(), WeakQuestionService.MAX_QUESTIONS);
        if (weakList.isEmpty()) {
            return "redirect:/test/weak";
        }
        List<Question> quesList = weakList.stream().map(WeakQuestionService.WeakQuestion::question).toList();

        model.addAttribute("answerListForm", new AnswerListForm());
        session.setAttribute("ans", buildAnswerFormList(quesList.size()));
        session.setAttribute("question", quesList);
        startTimer(quesList.size(), timer);
        addTimer(model);
        return "test/show_test";
    }

    // 復習問題（直近で間違えた問題）の一覧画面
    @RequestMapping(path = "/test/review", method = RequestMethod.GET)
    public String selectReview(Model model) {
        session.removeAttribute("question");
        session.removeAttribute("ans");
        session.removeAttribute("testFlag");

        model.addAttribute("reviewList",
                weakQuestionService.pickRecentMistakes(currentUser(), MAX_QUESTIONS));
        return "test/select_review";
    }

    // 復習問題テスト解答画面
    @RequestMapping(path = "/test/review/start", method = RequestMethod.GET)
    public String startReview(Model model) {
        List<Question> quesList = weakQuestionService.pickRecentMistakes(currentUser(), MAX_QUESTIONS);
        if (quesList.isEmpty()) {
            return "redirect:/test/review";
        }
        Collections.shuffle(quesList);

        model.addAttribute("answerListForm", new AnswerListForm());
        session.setAttribute("ans", buildAnswerFormList(quesList.size()));
        session.setAttribute("question", quesList);
        clearTimer();
        return "test/show_test";
    }

    // テスト解答確認画面
    @RequestMapping(path = "/test/check", method = RequestMethod.POST)
    public String checkTest(@Valid @ModelAttribute AnswerListForm answerForm,
                            BindingResult result, Model model) {
        if (session.getAttribute("question") == null) {
            return "redirect:/mypage";
        }
        session.setAttribute("ans", answerForm.getResults());
        addTimer(model);

        if (result.hasErrors()) {
            return "test/show_test";
        }
        return "test/review_test";
    }

    // テスト解答画面に戻る
    @RequestMapping(path = "/test/back", method = RequestMethod.POST)
    public String backTest(@ModelAttribute AnswerListForm ans, Model model) {
        if (session.getAttribute("question") == null) {
            return "redirect:/mypage";
        }
        addTimer(model);
        return "test/show_test";
    }

    // 制限時間切れ：未回答は「わからない」として、そのまま採点する
    @RequestMapping(path = "/test/timeup", method = RequestMethod.POST)
    public String timeUp(@ModelAttribute AnswerListForm answerForm, Model model) {
        List<?> questions = (List<?>) session.getAttribute("question");
        List<AnswerForm> results = answerForm.getResults();
        if (session.getAttribute(TIMER_START) == null || questions == null
                || results == null || results.size() != questions.size()) {
            return "redirect:/mypage";
        }
        for (AnswerForm a : results) {
            if (a.getAnsOption() == null || a.getAnsOption() < 1 || a.getAnsOption() > 4) {
                a.setAnsOption(4);
            }
        }
        session.setAttribute("ans", results);
        return resulttTest(model, new AnswerListForm());
    }

    // 解答結果確認画面
    @RequestMapping(path = "/test/result", method = RequestMethod.POST)
    public String resulttTest(Model model, AnswerListForm ans) {

        CustomerBean userBean =
                (CustomerBean) session.getAttribute("user");

        Customer user =
                customerRepository.getReferenceById(userBean.getUserId());

        // 再計算前のレベル
        int previousLevel = user.getUserLevel();
        
        @SuppressWarnings("unchecked")
        List<AnswerForm> results = (List<AnswerForm>) session.getAttribute("ans");
        if (results == null || results.isEmpty() || session.getAttribute("question") == null) {
            return "redirect:/mypage";
        }

        int qCount  = results.size();
        int caCount = 0;
        int[] cateCount  = {0, 0, 0, 0};
        int[] categCount = {0, 0, 0, 0};
        LocalDate today = LocalDate.now();

        Map<Integer, String> explanations = new LinkedHashMap<>();
        for (Question q : quesRpstry.findAllById(
                results.stream().map(AnswerForm::getQuestionId).toList())) {
            explanations.put(q.getQuestionId(), q.getExplanation());
        }

        for (AnswerForm aForm : results) {
            aForm.setExplanation(explanations.get(aForm.getQuestionId()));
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
            answer.setAnsCount(1);
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
        Customer updatedUser =
                customerRepository.getReferenceById(user.getUserId());

        // 現在のレベルと累計EXP
        int currentLevel = updatedUser.getUserLevel();
        long totalExp = updatedUser.getTotalExp() == null
                ? 0L
                : updatedUser.getTotalExp();

        // 現在のレベル内での進捗（0〜14 EXP）
        int expInLevel = (int) (totalExp % 15L);

        // 次のレベルまでに必要なEXP
        int expRemaining = 15 - expInLevel;

        // EXPバーの進捗率
        int expProgressPercent = expInLevel * 100 / 15;

        // レベルアップ判定
        boolean leveledUp = currentLevel > previousLevel;

        // 満点判定
        boolean isPerfect = qCount > 0 && caCount == qCount;
        
        // 正解率に応じたキャラクターとメッセージ
        String scoreCharacter = null;
        String characterMessage = "";

        if (qCount > 0) {
            if (caCount == qCount) {
                // 満点
                scoreCharacter = "suta_v_happy.png";
                characterMessage = "満点！すばらしいね！努力が実ったね！";

            } else if (caCount * 4 <= qCount) {
                // 正解率25%以下
                scoreCharacter = "suta_sad.png";
                characterMessage = "大丈夫！ここから一歩ずつ成長していこう！";

            } else if (caCount * 2 <= qCount) {
                // 正解率25%超～50%以下
                scoreCharacter = "suta_cry.png";
                characterMessage = "惜しい！間違えた問題を復習して、次はもっと正解しよう！";

            } else if (caCount * 4 < qCount * 3) {
                // 正解率50%超～75%未満
                scoreCharacter = "suta_amaze.png";
                characterMessage = "いい調子！もう少しでさらに上を目指せるよ！";

            } else {
                // 正解率75%以上、満点未満
                scoreCharacter = "suta_happy.png";
                characterMessage = "すごい！この調子で満点を目指そう！";
            }
        }

        // レベルアップ時は、専用キャラクターとメッセージを優先
        String displayCharacter =
                leveledUp ? "suta_star.png" : scoreCharacter;

        if (leveledUp) {
            characterMessage = "レベルアップ！毎日の努力が実ったね！";
        }
                      
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("cateCorrect",    categCount);
        view.put("cateCount",      cateCount);
        view.put("qc",             qCount);
        view.put("ca",             caCount);
        view.put("ans",            results);
        view.put("currentLevel",   currentLevel);
        // EXPバー表示用のデータ
        view.put("totalExp", totalExp);
        view.put("expInLevel", expInLevel);
        view.put("expRemaining", expRemaining);
        view.put("expProgressPercent", expProgressPercent);

        view.put("isPerfect",      isPerfect);
        view.put("newAchievements", newAchievements);
        view.put("previousLevel", previousLevel);
        view.put("displayCharacter", displayCharacter);
        view.put("leveledUp", leveledUp);
        view.put("characterMessage", characterMessage);
        
        session.setAttribute(RESULT_VIEW, view);

        session.removeAttribute("question");
        session.removeAttribute("ans");
        clearTimer();

        // 再読み込みやブラウザバックでの二重登録を防ぐため、リダイレクトで結果を表示する
        return "redirect:/test/result";
        
        
    }

    // 解答結果の表示（保存済みの結果をセッションから読む）
    @RequestMapping(path = "/test/result", method = RequestMethod.GET)
    public String showResult(Model model) {
        @SuppressWarnings("unchecked")
        Map<String, Object> view = (Map<String, Object>) session.getAttribute(RESULT_VIEW);
        if (view == null) {
            return "redirect:/mypage";
        }
        model.addAllAttributes(view);
        return "test/result_test";
    }

    // ---- ユーティリティ ----
    private Customer currentUser() {
        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        return customerRepository.getReferenceById(userBean.getUserId());
    }

    static final int MAX_QUESTIONS = 15;
    static final int SECONDS_PER_QUESTION = 25;
    static final String TIMER_START = "timerStartMillis";
    static final String TIMER_LIMIT = "timerLimitMillis";
    static final String RESULT_VIEW = "testResult";

    // タイマーは出題開始時の選択で決まり、解答中は変更できない
    private void startTimer(int questionCount, boolean enabled) {
        if (!enabled) {
            clearTimer();
            return;
        }
        session.setAttribute(TIMER_START, System.currentTimeMillis());
        session.setAttribute(TIMER_LIMIT, questionCount * SECONDS_PER_QUESTION * 1000L);
    }

    private void clearTimer() {
        session.removeAttribute(TIMER_START);
        session.removeAttribute(TIMER_LIMIT);
    }

    // タイマー対象のテストのときだけ、残り時間をテンプレートに渡す
    private void addTimer(Model model) {
        Long start = (Long) session.getAttribute(TIMER_START);
        Long limit = (Long) session.getAttribute(TIMER_LIMIT);
        if (start == null || limit == null) {
            return;
        }
        model.addAttribute("timerTotalMillis", limit);
        model.addAttribute("timerRemainingMillis", Math.max(0L, start + limit - System.currentTimeMillis()));
    }

    // 全件の中からランダムに最大15問を抽出（出題順もランダム）
    static List<Question> pickRandom(List<Question> all) {
        List<Question> shuffled = new ArrayList<>(all);
        Collections.shuffle(shuffled);
        return shuffled.size() > MAX_QUESTIONS ? new ArrayList<>(shuffled.subList(0, MAX_QUESTIONS)) : shuffled;
    }

    // カテゴリごとにシャッフルし、順番に1問ずつ取り出して偏りを防ぐ
    static List<Question> pickBalanced(List<Question> all) {
        Map<Integer, List<Question>> byCategory = new LinkedHashMap<>();
        for (Question q : all) {
            byCategory.computeIfAbsent(q.getCategory().getCategoryId(), k -> new ArrayList<>()).add(q);
        }
        List<List<Question>> groups = new ArrayList<>(byCategory.values());
        groups.forEach(Collections::shuffle);

        List<Question> picked = new ArrayList<>();
        for (int i = 0; picked.size() < MAX_QUESTIONS; i++) {
            boolean added = false;
            for (List<Question> g : groups) {
                if (i < g.size() && picked.size() < MAX_QUESTIONS) {
                    picked.add(g.get(i));
                    added = true;
                }
            }
            if (!added) break;
        }
        Collections.shuffle(picked);
        return picked;
    }

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
