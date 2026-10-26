package jp.co.sss.pr.mypage_function;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.repository.CategoryRepository;

@Controller
public class FriendPageController {

    private final CategoryRepository categoryRepository;

    public FriendPageController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/friends")
    public String friends(HttpSession session, Model model) {
        CustomerBean user = (CustomerBean) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        if (!Integer.valueOf(0).equals(user.getPermission())) {
            return "redirect:/mypage";
        }

        model.addAttribute("categoryList", categoryRepository.findAll());
        return "friends/index";
    }
}
