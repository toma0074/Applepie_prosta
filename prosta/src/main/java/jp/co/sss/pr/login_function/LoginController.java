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

    // 最初画面
    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String index(@ModelAttribute CustomerForm customerform) {
        session.invalidate();
        return "login/index";
    }

    // ログイン画面表示
    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String loginInput(@ModelAttribute CustomerForm customerForm) {
        return "login/login";
    }

    // ログイン処理
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(@Valid @ModelAttribute CustomerForm customerForm,
                        BindingResult result, HttpSession session, Model model) {

        if (result.hasErrors()) {
            return "login/login";
        }

        String userName = customerForm.getUserName();
        String userPass = customerForm.getUserPass();
        Customer customer = customerRepository.findByUserNameAndUserPass(userName, userPass);

        if (customer != null && !Boolean.TRUE.equals(customer.getDeleteFlag())) {
            // セッションに保存
            CustomerBean customerBean = new CustomerBean();
            customerBean.setUserId(customer.getUserId());
            customerBean.setUserName(customer.getUserName());
            customerBean.setUserPass(customer.getUserPass());
            customerBean.setPermission(customer.getPermission());
            customerBean.setDeleteFlag(customer.getDeleteFlag());
            session.setAttribute("user", customerBean);

            // ログインEXP付与・連続ログイン更新
            List<Achievement> newAchievements = levelService.processLogin(customer);
            if (!newAchievements.isEmpty()) {
                session.setAttribute("newAchievements", newAchievements);
            }

            return "redirect:/mypage";

        } else {
            model.addAttribute("errMessage", "ユーザー名、またはパスワードが間違っています。");
            return "login/login";
        }
    }

    // ログアウト
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout() {
        session.invalidate();
        return "redirect:/";
    }

    // 新規登録画面
    @RequestMapping(path = "/regist", method = RequestMethod.GET)
    public String inputRegist(@ModelAttribute CustomerForm customerForm) {
        return "login/user_regist";
    }

    // 新規登録入力チェック＆完了
    @RequestMapping(path = "/regist/check", method = RequestMethod.POST)
    public String checkRegist(@Valid @ModelAttribute CustomerForm customerForm,
                              BindingResult result, Model model) {

        Customer logCustomer = customerRepository.findByUserName(customerForm.getUserName());

        if (result.hasErrors()) {
            return "login/user_regist";
        } else if (logCustomer != null) {
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

        // 新規登録時もログインEXP付与・UserStatus初期化
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
    public String loginError(@ModelAttribute CustomerForm customerform) {
        session.invalidate();
        return "login_error";
    }
}
