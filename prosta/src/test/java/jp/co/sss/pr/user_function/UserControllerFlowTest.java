package jp.co.sss.pr.user_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.service.PasswordService;

/** ユーザー情報更新の正常系／異常系（権限・重複名・入力チェック） */
class UserControllerFlowTest {

    private CustomerRepository repo;
    private MockHttpSession session;
    private UserController controller;
    private LocalValidatorFactoryBean validator;
    private final PasswordService passwordService = new PasswordService();

    @BeforeEach
    void setUp() {
        repo = mock(CustomerRepository.class);
        session = new MockHttpSession();
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        controller = new UserController();
        controller.userRepository = repo;
        controller.passwordService = passwordService;
        controller.session = session;
        controller.validator = validator;
    }

    private Customer customer(int id, String name, int permission) {
        Customer c = new Customer();
        c.setUserId(id);
        c.setUserName(name);
        c.setPermission(permission);
        c.setDeleteFlag(false);
        c.setUserPass(passwordService.encode("oldpass1"));
        when(repo.findByUserId(id)).thenReturn(c);
        return c;
    }

    private void loginAs(int id, String name, int permission) {
        CustomerBean bean = new CustomerBean();
        bean.setUserId(id);
        bean.setUserName(name);
        bean.setPermission(permission);
        session.setAttribute("user", bean);
    }

    private CustomerForm form(Integer id, String name, String pass, Integer permission) {
        CustomerForm f = new CustomerForm();
        f.setUserId(id);
        f.setUserName(name);
        f.setUserPass(pass);
        f.setPermission(permission);
        return f;
    }

    private BindingResult validated(CustomerForm f) {
        BindingResult r = new BeanPropertyBindingResult(f, "user");
        validator.validate(f, r);
        return r;
    }

    private void assertStatus(HttpStatus expected, Runnable action) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expected, e.getStatusCode());
    }

    // ---- 入力画面 ----

    @Test
    void inputShowsOwnInfoWhenNoIdIsGiven() {
        customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("user/user_update_input", controller.userUpdateInput(model, new CustomerForm()));

        assertEquals("taro", ((CustomerBean) model.get("user")).getUserName());
        assertEquals("/mypage", model.get("returnUrl"));
    }

    @Test
    void inputKeepsTheNameBeingEditedWhenComingBackFromConfirmation() {
        customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.userUpdateInput(model, form(1, "jiro", null, null));

        assertEquals("jiro", ((CustomerBean) model.get("user")).getUserName());
    }

    @Test
    void generalUserCannotOpenAnotherUsersInput() {
        customer(1, "taro", 0);
        customer(2, "hanako", 0);
        loginAs(1, "taro", 0);

        assertStatus(HttpStatus.FORBIDDEN,
                () -> controller.userUpdateInput(new ExtendedModelMap(), form(2, null, null, null)));
    }

    @Test
    void adminCanOpenAnotherUsersInputAndDefaultsToUserListReturn() {
        customer(9, "admin", 1);
        customer(2, "hanako", 0);
        loginAs(9, "admin", 1);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.userUpdateInput(model, form(2, null, null, null));

        assertEquals("hanako", ((CustomerBean) model.get("user")).getUserName());
        assertEquals("/user/list", model.get("returnUrl"));
    }

    @Test
    void inputForUnknownUserIsNotFound() {
        customer(9, "admin", 1);
        when(repo.findByUserId(99)).thenReturn(null);
        loginAs(9, "admin", 1);

        assertStatus(HttpStatus.NOT_FOUND,
                () -> controller.userUpdateInput(new ExtendedModelMap(), form(99, null, null, null)));
    }

    @Test
    void untrustedReturnUrlFallsBackToTheDefault() {
        customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        ExtendedModelMap model = new ExtendedModelMap();
        CustomerForm f = new CustomerForm();
        f.setReturnUrl("http://evil.example/");

        controller.userUpdateInput(model, f);

        assertEquals("/mypage", model.get("returnUrl"));
    }

    // ---- 確認 ----

    @Test
    void checkRejectsADuplicateUserName() {
        customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        when(repo.existsByUserNameAndUserIdNot("hanako", 1)).thenReturn(true);
        CustomerForm f = form(1, "hanako", "pass1234", 0);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.userUpdateCheck(f, validated(f), model);

        assertEquals("user/user_update_input", view);
        assertEquals("既に使用されているユーザー名です。", model.get("err"));
        assertNull(session.getAttribute("pendingUserPass"));
    }

    @Test
    void checkAllowsKeepingYourOwnName() {
        customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        when(repo.existsByUserNameAndUserIdNot("taro", 1)).thenReturn(false);
        CustomerForm f = form(1, "taro", "pass1234", 0);

        assertEquals("user/user_update_check",
                controller.userUpdateCheck(f, validated(f), new ExtendedModelMap()));
        assertEquals("pass1234", session.getAttribute("pendingUserPass"));
    }

    @Test
    void checkRejectsInvalidInputWithoutTouchingTheSession() {
        customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        String[][] bad = { { "", "pass1234" }, { "123456789", "pass1234" }, { "taro", "abc" },
                { "taro", "pass 1234" }, { "taro", "" } };
        for (String[] b : bad) {
            CustomerForm f = form(1, b[0], b[1], 0);
            assertEquals("user/user_update_input",
                    controller.userUpdateCheck(f, validated(f), new ExtendedModelMap()));
        }
        assertNull(session.getAttribute("pendingUserPass"));
    }

    @Test
    void checkForcesTheTargetToTheLoggedInUserWhenIdIsBlank() {
        customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        CustomerForm f = form(null, "taro", "pass1234", 0);

        controller.userUpdateCheck(f, validated(f), new ExtendedModelMap());

        assertEquals(1, f.getUserId());
        assertEquals(1, session.getAttribute("pendingUserId"));
    }

    @Test
    void generalUserCannotCheckAnotherUsersUpdate() {
        customer(1, "taro", 0);
        customer(2, "hanako", 0);
        loginAs(1, "taro", 0);
        CustomerForm f = form(2, "x", "pass1234", 0);

        assertStatus(HttpStatus.FORBIDDEN,
                () -> controller.userUpdateCheck(f, validated(f), new ExtendedModelMap()));
    }

    // ---- 完了 ----

    private void pend(int id, String pass) {
        session.setAttribute("pendingUserPass", pass);
        session.setAttribute("pendingUserId", id);
    }

    @Test
    void completeUpdatesNameAndHashedPasswordAndRefreshesTheSessionUser() {
        Customer me = customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        pend(1, "newpass1");
        CustomerForm f = form(1, "jiro", null, 0);

        String view = controller.userUpdateComplete(f, new BeanPropertyBindingResult(f, "user"),
                new ExtendedModelMap());

        assertEquals("user/user_update_complete", view);
        assertEquals("jiro", me.getUserName());
        assertTrue(passwordService.matches("newpass1", me.getUserPass()));
        verify(repo).save(me);
        assertEquals("jiro", ((CustomerBean) session.getAttribute("user")).getUserName());
        assertNull(session.getAttribute("pendingUserPass"));
    }

    @Test
    void completeRejectsADuplicateNameEvenAfterConfirmation() {
        Customer me = customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        pend(1, "newpass1");
        customer(2, "hanako", 0);
        when(repo.existsByUserNameAndUserIdNot("hanako", 1)).thenReturn(true);
        CustomerForm f = form(1, "hanako", null, 0);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.userUpdateComplete(f, new BeanPropertyBindingResult(f, "user"), model);

        assertEquals("user/user_update_input", view);
        assertEquals("taro", me.getUserName());
        verify(repo, never()).save(any());
    }

    @Test
    void completeFailsValidationWhenPendingPasswordIsInvalid() {
        customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        pend(1, "ab");
        CustomerForm f = form(1, "taro", null, 0);

        assertEquals("user/user_update_input",
                controller.userUpdateComplete(f, new BeanPropertyBindingResult(f, "user"),
                        new ExtendedModelMap()));
        verify(repo, never()).save(any());
    }

    @Test
    void completeIgnoresPendingValuesForAnotherUser() {
        customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        pend(2, "newpass1");
        CustomerForm f = form(1, "taro", null, 0);

        String view = controller.userUpdateComplete(f, new BeanPropertyBindingResult(f, "user"),
                new ExtendedModelMap());

        assertTrue(view.startsWith("redirect:/user/update/input?userId=1"));
        verify(repo, never()).save(any());
    }

    @Test
    void generalUserCannotChangeTheirOwnPermission() {
        Customer me = customer(1, "taro", 0);
        loginAs(1, "taro", 0);
        pend(1, "newpass1");
        CustomerForm f = form(1, "taro", null, 1);

        controller.userUpdateComplete(f, new BeanPropertyBindingResult(f, "user"), new ExtendedModelMap());

        assertEquals(0, me.getPermission());
    }

    @Test
    void adminCanChangeAnotherUsersPermissionButNotToAnInvalidValue() {
        customer(9, "admin", 1);
        Customer target = customer(2, "hanako", 0);
        loginAs(9, "admin", 1);

        pend(2, "newpass1");
        CustomerForm ok = form(2, "hanako", null, 1);
        controller.userUpdateComplete(ok, new BeanPropertyBindingResult(ok, "user"), new ExtendedModelMap());
        assertEquals(1, target.getPermission());

        pend(2, "newpass1");
        CustomerForm bad = form(2, "hanako", null, 5);
        controller.userUpdateComplete(bad, new BeanPropertyBindingResult(bad, "user"), new ExtendedModelMap());
        assertEquals(1, target.getPermission());
    }

    @Test
    void adminEditingAnotherUserDoesNotChangeTheirOwnSession() {
        customer(9, "admin", 1);
        customer(2, "hanako", 0);
        loginAs(9, "admin", 1);
        pend(2, "newpass1");
        CustomerForm f = form(2, "hanako2", null, 0);

        controller.userUpdateComplete(f, new BeanPropertyBindingResult(f, "user"), new ExtendedModelMap());

        assertEquals("admin", ((CustomerBean) session.getAttribute("user")).getUserName());
        assertFalse(((CustomerBean) session.getAttribute("user")).getPermission() == 0);
    }
}
