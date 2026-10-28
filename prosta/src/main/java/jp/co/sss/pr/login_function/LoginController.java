package jp.co.sss.pr.login_function;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryStudySummary;
import jp.co.sss.pr.repository.DailyStudySummary;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.service.LevelService;
import jp.co.sss.pr.service.PasswordService;

@Controller
public class LoginController {

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    AnswerRepository answerRepository;

    @Autowired
    HttpSession session;

    @Autowired
    LevelService levelService;

    @Autowired
    PasswordService passwordService;

    @Autowired
    HttpServletRequest request;

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

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

        List<Customer> activeCustomers = customerRepository.findAllByUserName(customerForm.getUserName()).stream()
                .filter(customer -> !Boolean.TRUE.equals(customer.getDeleteFlag()))
                .filter(customer -> passwordService.matches(customerForm.getUserPass(), customer.getUserPass()))
                .toList();

        if (activeCustomers.size() == 1) {
            Customer customer = activeCustomers.get(0);
            upgradeLegacyPassword(customer, customerForm.getUserPass());
            request.changeSessionId();
            session.setAttribute("user", toBean(customer));
            restoreBackground(customer, session);

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
        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        if (userBean == null) {
            return "redirect:/";
        }
        Customer customer = customerRepository.getReferenceById(userBean.getUserId());
        LocalDate today = LocalDate.now();

        long todayAnswers = 0;
        long todayCorrect = 0;
        for (DailyStudySummary daily : answerRepository.summarizeDailyStudy(customer, today, today)) {
            todayAnswers += daily.getAnswerCount();
            todayCorrect += daily.getCorrectCount();
        }

        long totalAnswers = 0;
        long totalCorrect = 0;
        CategoryStudySummary weakest = null;
        double weakestRate = 101;
        for (CategoryStudySummary s : answerRepository.summarizeByCategory(customer)) {
            totalAnswers += s.getAnswerCount();
            totalCorrect += s.getCorrectCount();
            double rate = s.getAnswerCount() == 0 ? 101 : s.getCorrectCount() * 100.0 / s.getAnswerCount();
            if (rate < weakestRate) {
                weakestRate = rate;
                weakest = s;
            }
        }

        model.addAttribute("todayAnswers", todayAnswers);
        model.addAttribute("todayCorrect", todayCorrect);
        model.addAttribute("todayAccuracy",
                todayAnswers == 0 ? 0 : Math.round(todayCorrect * 100.0 / todayAnswers));
        model.addAttribute("totalAnswers", totalAnswers);
        model.addAttribute("totalAccuracy",
                totalAnswers == 0 ? 0 : Math.round(totalCorrect * 100.0 / totalAnswers));
        model.addAttribute("weakCategory", weakest == null ? null : weakest.getCategoryName());
        model.addAttribute("weakAccuracy", weakest == null ? 0 : Math.round(weakestRate));
        return "logout/logout_analyze";
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
        boolean nameTaken = customerRepository.existsByUserName(customerForm.getUserName());

        if (result.hasErrors()) {
            return "login/user_regist";
        }
        if (nameTaken) {
            model.addAttribute("errMessage", "既に使用されているユーザー名です。");
            return "login/user_regist";
        }

        Customer customer = new Customer();
        customer.setUserName(customerForm.getUserName());
        customer.setUserPass(passwordService.encode(customerForm.getUserPass()));
        customer.setPermission(0);
        customer.setDeleteFlag(false);
        customer.setLoginDate(LocalDate.now());
        customer.setLoginCount(1);
        customer.setLoginFlag(true);
        customer.setUserLevel(1);
        customer.setConsecutiveLoginDays(1);
        customer.setLastSeenAt(LocalDateTime.now());
        customer = customerRepository.save(customer);

        request.changeSessionId();
        session.setAttribute("user", toBean(customer));
        restoreBackground(customer, session);

        return "redirect:/mypage";
    }

    // パスワードは保持しない
    private CustomerBean toBean(Customer customer) {
        CustomerBean customerBean = new CustomerBean();
        customerBean.setUserId(customer.getUserId());
        customerBean.setUserName(customer.getUserName());
        customerBean.setPermission(customer.getPermission());
        customerBean.setDeleteFlag(customer.getDeleteFlag());
        return customerBean;
    }

    private void restoreBackground(Customer customer, HttpSession session) {
        String backgroundClass = customer.getBackgroundClass();
        String backgroundColor = customer.getBackgroundColor();
        if ("bg-color".equals(backgroundClass) && isHexColor(backgroundColor)) {
            session.setAttribute("backgroundClass", backgroundClass);
            session.setAttribute("backgroundColor", backgroundColor);
        } else if (isImageBackground(backgroundClass)) {
            session.setAttribute("backgroundClass", backgroundClass);
            session.removeAttribute("backgroundColor");
        } else {
            session.removeAttribute("backgroundClass");
            session.removeAttribute("backgroundColor");
        }
    }

    private boolean isImageBackground(String value) {
        return "bg-grass".equals(value) || "bg-sea".equals(value) || "bg-sky".equals(value)
                || "bg-sakura".equals(value) || "bg-forest".equals(value) || "bg-space".equals(value);
    }

    private boolean isHexColor(String value) {
        return value != null && value.matches("#[0-9A-Fa-f]{6}");
    }

    // 平文保存の旧データは、ログインに成功した時点でハッシュ化し直す
    private void upgradeLegacyPassword(Customer customer, String rawPassword) {
        if (!passwordService.isLegacy(customer.getUserPass())) {
            return;
        }
        String original = customer.getUserPass();
        try {
            customer.setUserPass(passwordService.encode(rawPassword));
            customerRepository.save(customer);
        } catch (RuntimeException e) {
            // 後続の保存処理に未ハッシュ化の値を持ち越さないよう元に戻す
            customer.setUserPass(original);
            log.warn("パスワードのハッシュ化に失敗しました。customer.user_pass の列幅を確認してください。", e);
        }
    }

    @RequestMapping(path = "/login/error", method = RequestMethod.GET)
    public String loginError(@ModelAttribute CustomerForm customerForm) {
        session.invalidate();
        return "login_error";
    }
}
