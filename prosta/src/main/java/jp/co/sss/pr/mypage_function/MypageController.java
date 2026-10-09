package jp.co.sss.pr.mypage_function;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;
import jp.co.sss.pr.service.LevelService;

@Controller
public class MypageController {

    @Autowired
    AnswerRepository answerRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    LevelService levelService;

    @Autowired
    UserAchievementRepository userAchievementRepository;

    @RequestMapping(path = "/mypage", method = RequestMethod.GET)
    public String mypage(Model model, HttpSession session,
                         @RequestParam(name = "sort", required = false) Integer sort) {

        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        if (userBean == null) {
            return "redirect:/";
        }

        Customer user = customerRepository.getReferenceById(userBean.getUserId());

        // 解答履歴取得
        List<Answer> ansList;
        if (sort == null || sort == 1) {
            sort = 1;
            ansList = answerRepository.findAllByCustomerOrderByAnsIdDesc(user);
        } else if (sort == 2) {
            ansList = answerRepository.findAllByCustomerOrderByAnsIdAsc(user);
            model.addAttribute("sort", 2);
        } else {
            ansList = answerRepository.findAllByCustomerOrderByAnsIdDesc(user);
        }
        model.addAttribute("answerlist", ansList);

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