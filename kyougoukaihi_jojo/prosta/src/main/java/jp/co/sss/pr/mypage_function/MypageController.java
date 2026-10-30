package jp.co.sss.pr.mypage_function;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.UserAchievement;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.QuestionAttemptStat;
import jp.co.sss.pr.repository.UserAchievementRepository;
import jp.co.sss.pr.service.LevelService;
import jp.co.sss.pr.service.QuestService;

@Controller
public class MypageController {

    private static final int HISTORY_PAGE_SIZE = 6;

    @Autowired
    AnswerRepository answerRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    LevelService levelService;

    @Autowired
    QuestService questService;

    @Autowired
    UserAchievementRepository userAchievementRepository;

    @RequestMapping(path = "/mypage", method = RequestMethod.GET)
    public String mypage(Model model, HttpSession session) {

        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        if (userBean == null) {
            return "redirect:/";
        }

        Customer user = customerRepository.getReferenceById(userBean.getUserId());
     // 累計EXPを取得
        long totalExp = user.getTotalExp();

        // 累計EXPからLEVELを計算
        int currentLevel = (int) (totalExp / 15L) + 1;

        // 現在のLEVEL内のEXP
        int expInLevel = (int) (totalExp % 15L);

        // 次のLEVELまでに必要なEXP
        int expRemaining = 15 - expInLevel;

        // 進捗バーの割合
        int expProgressPercent = expInLevel * 100 / 15;

        // HTMLへ渡す
        model.addAttribute("totalExp", totalExp);
        model.addAttribute("currentLevel", currentLevel);
        model.addAttribute("expInLevel", expInLevel);
        model.addAttribute("expRemaining", expRemaining);
        model.addAttribute("expProgressPercent", expProgressPercent);
        
     // ログイン日数
        model.addAttribute("loginCount", user.getLoginCount());

        // 累計正解数
        model.addAttribute(
            "totalCorrect",
            answerRepository.countByCustomerAndCorrectFlagTrue(user)
        );

        // 満点回数
        model.addAttribute(
            "perfectCount",
            answerRepository.countPerfectDaysByCustomer(user)
        );
        // 実績一覧（解除済み）
        List<UserAchievement> achievements =
                userAchievementRepository.findByCustomerOrderByUnlockedDateDesc(user);
        model.addAttribute("achievements", achievements);

        // 新規解除実績フラッシュ
        if (session.getAttribute("newAchievements") != null) {
            model.addAttribute("newAchievements", session.getAttribute("newAchievements"));
            session.removeAttribute("newAchievements");
        }

        return "mypage/mypage";
    }

    // 解答履歴（マイページ経由で表示）
    @RequestMapping(path = "/mypage/history", method = RequestMethod.GET)
    public String history(Model model, HttpSession session,
                          @RequestParam(name = "sort", required = false) Integer sort,
                          @RequestParam(required = false) Integer categoryId,
                          @RequestParam(required = false) Integer difficulty,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer correct,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                          @RequestParam(defaultValue = "0") int page) {

        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        if (userBean == null) {
            return "redirect:/";
        }

        Customer user = customerRepository.getReferenceById(userBean.getUserId());

        int sortType = (sort != null && sort == 2) ? 2 : 1;
        String kw = keyword == null ? "" : keyword.trim();
        Integer correctFilter = (correct != null && (correct == 0 || correct == 1)) ? correct : null;
        Sort order = sortType == 2 ? Sort.by("ansId").ascending() : Sort.by("ansId").descending();
        Page<Answer> result = answerRepository.searchHistory(user, categoryId, difficulty,
                "%" + kw + "%", correctFilter, fromDate, toDate,
                PageRequest.of(Math.max(page, 0), HISTORY_PAGE_SIZE, order));
        model.addAttribute("answerlist", result.getContent());

        // 問題ごとの誤答回数（自分の全履歴ベース）
        Map<Integer, Long> wrongCounts = new HashMap<>();
        for (QuestionAttemptStat stat : answerRepository.summarizeByQuestion(user, 1)) {
            wrongCounts.put(stat.getQuestionId(), stat.getAttemptCount() - stat.getCorrectCount());
        }
        model.addAttribute("wrongCounts", wrongCounts);
        model.addAttribute("historyPage", result);
        model.addAttribute("sort", sortType);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("keyword", kw);
        model.addAttribute("correct", correctFilter);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "mypage/answer_history";
    }
}