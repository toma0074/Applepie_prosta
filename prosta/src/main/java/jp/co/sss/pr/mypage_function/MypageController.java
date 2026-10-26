package jp.co.sss.pr.mypage_function;

import java.time.LocalDate;
import java.util.List;

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
import jp.co.sss.pr.repository.UserAchievementRepository;
import jp.co.sss.pr.service.LevelService;

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
    UserAchievementRepository userAchievementRepository;

    @RequestMapping(path = "/mypage", method = RequestMethod.GET)
    public String mypage(Model model, HttpSession session,
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

        // 解答履歴取得（絞り込み＋ページング）
        int sortType = (sort != null && sort == 2) ? 2 : 1;
        String kw = keyword == null ? "" : keyword.trim();
        Integer correctFilter = (correct != null && (correct == 0 || correct == 1)) ? correct : null;
        Sort order = sortType == 2 ? Sort.by("ansId").ascending() : Sort.by("ansId").descending();
        Page<Answer> result = answerRepository.searchHistory(user, categoryId, difficulty,
                "%" + kw + "%", correctFilter, fromDate, toDate,
                PageRequest.of(Math.max(page, 0), HISTORY_PAGE_SIZE, order));
        model.addAttribute("answerlist", result.getContent());
        model.addAttribute("historyPage", result);
        model.addAttribute("sort", sortType);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("keyword", kw);
        model.addAttribute("correct", correctFilter);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        // レベル・ログイン情報（customer テーブルから直接取得）
        int currentLevel  = user.getUserLevel();
        int loginCount    = user.getLoginCount();
        int totalCorrect  = levelService.getTotalCorrect(user);
        int perfectCount  = levelService.getPerfectCount(user);

        model.addAttribute("currentLevel",  currentLevel);
        model.addAttribute("loginCount",    loginCount);
        model.addAttribute("totalCorrect",  totalCorrect);
        model.addAttribute("perfectCount",  perfectCount);

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
}