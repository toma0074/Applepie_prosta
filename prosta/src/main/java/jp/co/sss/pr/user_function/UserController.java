package jp.co.sss.pr.user_function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.service.PasswordService;
import jp.co.sss.pr.service.ReturnUrls;

@Controller
public class UserController {

    private static final Integer ADMIN = 1;
    private static final String PENDING_PASS = "pendingUserPass";
    private static final String PENDING_USER_ID = "pendingUserId";

    @Autowired
    Validator validator;

    @Autowired
    CustomerRepository userRepository;

    @Autowired
    PasswordService passwordService;

    @Autowired
    HttpSession session;

    @RequestMapping(path = "/user/update/input", method = RequestMethod.GET)
    public String userUpdateInput(Model model, CustomerForm userForm) {
        CustomerBean login = loginUser();
        Customer target = authorizedTarget(login, userForm.getUserId());

        // 確認画面から戻ったときは、入力途中の内容を引き継ぐ
        CustomerBean user = new CustomerBean();
        user.setUserId(target.getUserId());
        user.setUserName(userForm.getUserName() != null ? userForm.getUserName() : target.getUserName());
        user.setPermission(isAdmin(login) && userForm.getPermission() != null
                ? userForm.getPermission() : target.getPermission());
        user.setDeleteFlag(target.getDeleteFlag());

        model.addAttribute("user", user);
        addReturnUrl(model, userForm, login);
        return "user/user_update_input";
    }

    @RequestMapping(path = "/user/update/check", method = RequestMethod.POST)
    public String userUpdateCheck(@Valid @ModelAttribute("user") CustomerForm user,
                                  BindingResult result, Model model) {
        Customer target = authorizedTarget(loginUser(), user.getUserId());
        // 画面側の隠しフィールドが空でも、更新対象は確定した本人のIDにそろえる
        user.setUserId(target.getUserId());
        addReturnUrl(model, user, loginUser());

        if (result.hasErrors()) {
            return "user/user_update_input";
        }
        if (isNameTaken(user, target)) {
            model.addAttribute("err", "既に使用されているユーザー名です。");
            return "user/user_update_input";
        }
        // パスワードはHTMLに出さず、確認画面から確定までセッションで保持する
        session.setAttribute(PENDING_PASS, user.getUserPass());
        session.setAttribute(PENDING_USER_ID, user.getUserId());
        return "user/user_update_check";
    }

    @RequestMapping(path = "/user/update/complete", method = RequestMethod.POST)
    public String userUpdateComplete(@ModelAttribute("user") CustomerForm userForm,
                                     BindingResult result, Model model) {
        CustomerBean login = loginUser();
        Customer existing = authorizedTarget(login, userForm.getUserId());
        addReturnUrl(model, userForm, login);

        Object pendingPass = session.getAttribute(PENDING_PASS);
        Object pendingId = session.getAttribute(PENDING_USER_ID);
        session.removeAttribute(PENDING_PASS);
        session.removeAttribute(PENDING_USER_ID);
        if (!(pendingPass instanceof String) || !existing.getUserId().equals(pendingId)) {
            return "redirect:/user/update/input?userId=" + existing.getUserId()
                    + "&returnUrl=" + model.getAttribute("returnUrl");
        }
        userForm.setUserPass((String) pendingPass);
        validator.validate(userForm, result);

        if (result.hasErrors()) {
            return "user/user_update_input";
        }
        if (isNameTaken(userForm, existing)) {
            model.addAttribute("err", "既に使用されているユーザー名です。");
            return "user/user_update_input";
        }

        existing.setUserName(userForm.getUserName());
        existing.setUserPass(passwordService.encode(userForm.getUserPass()));
        // 権限を変更できるのは管理者だけ
        if (isAdmin(login) && (Integer.valueOf(0).equals(userForm.getPermission())
                || ADMIN.equals(userForm.getPermission()))) {
            existing.setPermission(userForm.getPermission());
        }
        userRepository.save(existing);

        if (existing.getUserId().equals(login.getUserId())) {
            login.setUserName(existing.getUserName());
            login.setPermission(existing.getPermission());
            session.setAttribute("user", login);
        }

        model.addAttribute("pm", login.getPermission());
        return "user/user_update_complete";
    }

    // 入口ごとの戻り先。未指定・不正な値は権限ごとの既定値にする
    private void addReturnUrl(Model model, CustomerForm form, CustomerBean login) {
        String fallback = isAdmin(login) ? "/user/list" : "/mypage";
        String url = ReturnUrls.resolve(form.getReturnUrl(), fallback);
        model.addAttribute("returnUrl", url);
        model.addAttribute("returnLabel", ReturnUrls.label(url));
    }

    private CustomerBean loginUser() {
        return (CustomerBean) session.getAttribute("user");
    }

    private boolean isAdmin(CustomerBean login) {
        return ADMIN.equals(login.getPermission());
    }

    // 一般ユーザーは自分の情報だけ、管理者は全員分を更新できる
    private Customer authorizedTarget(CustomerBean login, Integer requestedId) {
        Integer targetId = requestedId == null ? login.getUserId() : requestedId;
        if (!isAdmin(login) && !targetId.equals(login.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Customer target = userRepository.findByUserId(targetId);
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return target;
    }

    private boolean isNameTaken(CustomerForm form, Customer target) {
        return userRepository.existsByUserNameAndUserIdNot(form.getUserName(), target.getUserId());
    }
}
