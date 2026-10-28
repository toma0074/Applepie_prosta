package jp.co.sss.pr.user_function;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.CustomerRepository;

@Controller
public class UserAddController {

    @Autowired
    HttpSession session;

    @Autowired
    CustomerRepository userRepository;

    // ユーザー一覧表示(名前・権限・状態の複合検索)
    @RequestMapping(path = "/user/list", method = RequestMethod.GET)
    public String userListshow(Model model,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Integer permission,
            @RequestParam(required = false) Boolean deleteFlag) {
        String keyword = userName == null ? "" : userName.trim();
        List<Customer> userList = userRepository.findAll().stream()
                .filter(u -> keyword.isEmpty()
                        || (u.getUserName() != null && u.getUserName().contains(keyword)))
                .filter(u -> permission == null || permission.equals(u.getPermission()))
                .filter(u -> deleteFlag == null
                        || deleteFlag.equals(Boolean.TRUE.equals(u.getDeleteFlag())))
                .toList();
        model.addAttribute("customerForm", userList);
        model.addAttribute("userName", keyword);
        model.addAttribute("permission", permission);
        model.addAttribute("deleteFlag", deleteFlag);
        return "user/user_list";
    }

    // ユーザー削除確認
    @RequestMapping(path = "/user/delete/check", method = RequestMethod.POST)
    public String userDeleteCheck(@ModelAttribute CustomerForm customerForm, Model model) {
        CustomerBean loginUser = (CustomerBean) session.getAttribute("user");

        if (customerForm.getUserId().equals(loginUser.getUserId())) {
            model.addAttribute("errorMessage", "ログイン中のユーザーは操作できません");
            model.addAttribute("back", "一覧に戻る");
            return "user/user_list";

        } else if (Boolean.TRUE.equals(customerForm.getDeleteFlag())) {
            // ← Integer==1 の比較を Boolean.TRUE.equals() に変更
            model.addAttribute("errorMessage", "選択したユーザーは削除済みです");
            model.addAttribute("back", "一覧に戻る");
            return "user/user_list";
        }

        model.addAttribute("flag", "このユーザーを削除しますか?");
        return "user/user_delete_check";
    }

    // ユーザー復元確認
    @RequestMapping(path = "/user/restore/check", method = RequestMethod.POST)
    public String userRestoreCheck(@ModelAttribute CustomerForm customerForm, Model model) {
        CustomerBean loginUser = (CustomerBean) session.getAttribute("user");

        if (customerForm.getUserId().equals(loginUser.getUserId())) {
            model.addAttribute("errorMessage", "ログイン中のユーザーは操作できません");
            model.addAttribute("back", "一覧に戻る");
            return "user/user_list";

        } else if (!Boolean.TRUE.equals(customerForm.getDeleteFlag())) {
            // ← Integer==0 の比較を !Boolean.TRUE.equals() に変更
            model.addAttribute("errorMessage", "選択したユーザーはアクティブです");
            model.addAttribute("back", "一覧に戻る");
            return "user/user_list";
        }

        model.addAttribute("flag", "このユーザーを復元しますか?");
        return "user/user_delete_check";
    }

    // ユーザー削除/復元完了
    @RequestMapping(path = "/user/delete/complete", method = RequestMethod.POST)
    public String userDeleteComp(Model model, Integer userId) {
        Customer user = userId == null ? null : userRepository.findByUserId(userId);
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        String m = "";

        if (!Boolean.TRUE.equals(user.getDeleteFlag())) {
            // 現在アクティブ（false） → 削除（true）に変更
            user.setDeleteFlag(true);
            m = "さんを削除しました";
        } else {
            // 現在削除済み（true） → 復元（false）に変更
            user.setDeleteFlag(false);
            m = "さんを復元しました";
        }
        // ↑ switch(deleteFlag) は Boolean に使えないため if-else に変更

        userRepository.save(user);
        model.addAttribute("userName", user.getUserName() + m);

        return "user/user_delete_complete";
    }
}

