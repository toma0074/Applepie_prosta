package jp.co.sss.pr.mypage_function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.CustomerRepository;

@Controller
public class BackgroundController {

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/background")
    public String index(
            HttpSession session,
            Model model) {

        CustomerBean user =
                (CustomerBean) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        Customer customer =
                customerRepository
                .findById(user.getUserId())
                .orElse(null);

        if (customer == null) {
            return "redirect:/mypage";
        }

        model.addAttribute(
                "level",
                customer.getUserLevel());

        return "mypage/background_select";
    }

    @PostMapping("/background/change")
    public String change(
            @RequestParam String background,
            HttpSession session) {

        session.setAttribute("backgroundClass", background);
        session.removeAttribute("backgroundColor");

        return "redirect:/background";
    }
    
    @PostMapping("/background/changeColor")
    public String changeColor(
            @RequestParam String backgroundColor,
            HttpSession session) {

        session.setAttribute("backgroundColor", backgroundColor);
        session.setAttribute("backgroundClass", "bg-color");

        return "redirect:/background";
    }
    @PostMapping("/background/default")
    public String defaultBackground(
            HttpSession session) {

        session.removeAttribute("backgroundClass");
        session.removeAttribute("backgroundColor");

        return "redirect:/background";
    }
}