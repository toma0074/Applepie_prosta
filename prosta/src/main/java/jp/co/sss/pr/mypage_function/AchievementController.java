package jp.co.sss.pr.mypage_function;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.UserAchievement;
import jp.co.sss.pr.repository.AchievementRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;
import jp.co.sss.pr.service.LevelService;

/**
 * 実績一覧ページのController
 */
@Controller
public class AchievementController {

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    AchievementRepository achievementRepository;

    @Autowired
    UserAchievementRepository userAchievementRepository;

    @Autowired
    LevelService levelService;

    @RequestMapping(path = "/achievement", method = RequestMethod.GET)
    public String achievement(Model model, HttpSession session) {
        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        if (userBean == null) {
            return "redirect:/";
        }

        Customer user = customerRepository.getReferenceById(userBean.getUserId());

        // 全実績マスター
        List<Achievement> allAchievements = achievementRepository.findAll();

        // 解除済み実績
        List<UserAchievement> unlockedList =
                userAchievementRepository.findByCustomerOrderByUnlockedDateDesc(user);

        // 解除済みIDのセット
        List<Integer> unlockedIds = new ArrayList<>();
        for (UserAchievement ua : unlockedList) {
            unlockedIds.add(ua.getAchievement().getAchievementId());
        }

        // 統計情報（customer + answer テーブルから）
        int currentLevel = user.getUserLevel();
        int loginCount   = user.getLoginCount();
        int perfectCount = levelService.getPerfectCount(user);

        model.addAttribute("allAchievements",  allAchievements);
        model.addAttribute("unlockedList",     unlockedList);
        model.addAttribute("unlockedIds",      unlockedIds);
        model.addAttribute("currentLevel",     currentLevel);
        model.addAttribute("loginCount",       loginCount);
        model.addAttribute("perfectCount",     perfectCount);
        model.addAttribute("unlockedCount",    unlockedList.size());
        model.addAttribute("totalCount",       allAchievements.size());

        return "mypage/achievement";
    }
}
