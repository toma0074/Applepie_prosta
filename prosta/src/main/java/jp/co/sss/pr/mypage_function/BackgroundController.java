package jp.co.sss.pr.mypage_function;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.CustomerRepository;

@Controller
public class BackgroundController {

    private static final Map<String, Integer> BACKGROUND_LEVELS = Map.of(
            "bg-grass", 1,
            "bg-sea", 10,
            "bg-sky", 25,
            "bg-sakura", 50,
            "bg-forest", 75,
            "bg-space", 100);

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
        model.addAttribute("backgroundClass", customer.getBackgroundClass());
        model.addAttribute("backgroundColor", customer.getBackgroundColor());

        return "mypage/background_select";
    }

    @PostMapping("/background/change")
    public String change(
            @RequestParam String background,
            HttpSession session) {

        Customer customer = currentCustomer(session);
        Integer requiredLevel = BACKGROUND_LEVELS.get(background);
        if (requiredLevel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "背景の指定が無効です");
        }
        if (customer.getUserLevel() < requiredLevel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "この背景はまだ選択できません");
        }

        customer.setBackgroundClass(background);
        customer.setBackgroundColor(null);
        customerRepository.save(customer);
        session.setAttribute("backgroundClass", background);
        session.removeAttribute("backgroundColor");

        return "redirect:/background";
    }
    
    @PostMapping("/background/changeColor")
    public String changeColor(
            @RequestParam String backgroundColor,
            HttpSession session) {

        if (!isHexColor(backgroundColor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "背景色の指定が無効です");
        }
        Customer customer = currentCustomer(session);
        customer.setBackgroundClass("bg-color");
        String normalizedColor = backgroundColor.toUpperCase();
        customer.setBackgroundColor(normalizedColor);
        customerRepository.save(customer);
        session.setAttribute("backgroundColor", normalizedColor);
        session.setAttribute("backgroundClass", "bg-color");

        return "redirect:/background";
    }
<<<<<<< HEAD
    @PostMapping("/background/default")
    public String defaultBackground(
            HttpSession session) {

        session.removeAttribute("backgroundClass");
        session.removeAttribute("backgroundColor");

        return "redirect:/background";
=======

    private Customer currentCustomer(HttpSession session) {
        CustomerBean user = (CustomerBean) session.getAttribute("user");
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインが必要です");
        }
        return customerRepository.findById(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインが必要です"));
    }

    private boolean isHexColor(String value) {
        return value != null && value.matches("#[0-9A-Fa-f]{6}");
>>>>>>> 1cf35766ed15fe66278b0e0e1ad935be161f9706
    }
}