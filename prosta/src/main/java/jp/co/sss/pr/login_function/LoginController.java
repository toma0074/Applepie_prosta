package jp.co.sss.pr.login_function;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.service.LevelService;

@Controller
public class LoginController {

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    HttpSession session;

    @Autowired
    LevelService levelService;

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String index(@ModelAttribute CustomerForm customerForm) {
        session.invalidate();
        return "login/index";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String loginInput(@ModelAttribute CustomerForm customerForm) {
        return "login/login";
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(@Valid @ModelAttribute CustomerForm customerForm,
                        BindingResult result, HttpSession session, Model model) {
        if (result.hasErrors()) {
            return "login/login";
        }

        List<Customer> matchingCustomers = customerRepository.findAllByUserNameAndUserPass(
                customerForm.getUserName(), customerForm.getUserPass());
        List<Customer> activeCustomers = matchingCustomers.stream()
                .filter(customer -> !Boolean.TRUE.equals(customer.getDeleteFlag()))
                .toList();

        if (activeCustomers.size() == 1) {
            Customer customer = activeCustomers.get(0);
            CustomerBean customerBean = new CustomerBean();
            customerBean.setUserId(customer.getUserId());
            customerBean.setUserName(customer.getUserName());
            customerBean.setUserPass(customer.getUserPass());
            customerBean.setPermission(customer.getPermission());
            customerBean.setDeleteFlag(customer.getDeleteFlag());
            session.setAttribute("user", customerBean);

            List<Achievement> newAchievements = levelService.processLogin(customer);
            if (!newAchievements.isEmpty()) {
                session.setAttribute("newAchievements", newAchievements);
            }
            return "redirect:/mypage";
        }

        if (activeCustomers.size() > 1) {
            model.addAttribute("errMessage", "同じログイン情報のユーザーが複数登録されています。管理者にお問い合わせください。");
            return "login/login";
        }

        model.addAttribute("errMessage", "ユーザー名、またはパスワードが間違っています。");
        return "login/login";
    }

    @RequestMapping(path = "/logout/analyze", method = RequestMethod.GET)
    public String logoutAnalyze(Model model) {
        return "logout_analyze";
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout() {
        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        if (userBean != null) {
            customerRepository.findById(userBean.getUserId()).ifPresent(customer -> {
                customer.setLoginFlag(false);
                customer.setLastSeenAt(null);
                customerRepository.save(customer);
            });
        }
        session.invalidate();
        return "redirect:/";
    }

    @RequestMapping(path = "/regist", method = RequestMethod.GET)
    public String inputRegist(@ModelAttribute CustomerForm customerForm) {
        return "login/user_regist";
    }

    @RequestMapping(path = "/regist/check", method = RequestMethod.POST)
    public String checkRegist(@Valid @ModelAttribute CustomerForm customerForm,
                              BindingResult result, Model model) {
        Customer existingCustomer =
                customerRepository.findByUserName(customerForm.getUserName());

        if (result.hasErrors()) {
            return "login/user_regist";
        }
        if (existingCustomer != null) {
            model.addAttribute("errMessage", "既に使用されているユーザー名です。");
            return "login/user_regist";
        }

        Customer customer = new Customer();
        BeanUtils.copyProperties(customerForm, customer);
        customer.setPermission(1);
        customer.setDeleteFlag(false);
        customer.setLoginCount(0);
        customer.setLoginFlag(false);
        customer.setUserLevel(1);
        customer = customerRepository.save(customer);
        levelService.processLogin(customer);

        CustomerBean customerBean = new CustomerBean();
        customerBean.setUserId(customer.getUserId());
        customerBean.setUserName(customer.getUserName());
        customerBean.setUserPass(customer.getUserPass());
        customerBean.setPermission(customer.getPermission());
        customerBean.setDeleteFlag(customer.getDeleteFlag());
        session.setAttribute("user", customerBean);

        return "redirect:/mypage";
    }

    @RequestMapping(path = "/login/error", method = RequestMethod.GET)
    public String loginError(@ModelAttribute CustomerForm customerForm) {
        session.invalidate();
        return "login_error";
    }
}
