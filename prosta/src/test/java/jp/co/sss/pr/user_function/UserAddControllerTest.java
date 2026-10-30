package jp.co.sss.pr.user_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.CustomerRepository;

/** ユーザー一覧（検索）・削除／復元の正常系／異常系 */
class UserAddControllerTest {

    private CustomerRepository repo;
    private MockHttpSession session;
    private UserAddController controller;
    private Customer taro;
    private Customer hanako;
    private Customer deleted;

    @BeforeEach
    void setUp() {
        repo = mock(CustomerRepository.class);
        session = new MockHttpSession();
        controller = new UserAddController();
        controller.userRepository = repo;
        controller.session = session;

        taro = user(1, "taro", 0, false);
        hanako = user(2, "hanako", 1, false);
        deleted = user(3, "taroOld", 0, true);
        when(repo.findAll()).thenReturn(List.of(taro, hanako, deleted));

        CustomerBean login = new CustomerBean();
        login.setUserId(1);
        session.setAttribute("user", login);
    }

    private Customer user(int id, String name, int permission, boolean del) {
        Customer c = new Customer();
        c.setUserId(id);
        c.setUserName(name);
        c.setPermission(permission);
        c.setDeleteFlag(del);
        when(repo.findByUserId(id)).thenReturn(c);
        return c;
    }

    @SuppressWarnings("unchecked")
    private List<Customer> list(ExtendedModelMap model) {
        return (List<Customer>) model.get("customerForm");
    }

    private CustomerForm target(int id, boolean deleteFlag) {
        CustomerForm f = new CustomerForm();
        f.setUserId(id);
        f.setDeleteFlag(deleteFlag);
        return f;
    }

    // ---- 一覧・検索 ----

    @Test
    void listShowsEveryoneWithoutConditions() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("user/user_list", controller.userListshow(model, null, null, null));
        assertEquals(3, list(model).size());
    }

    @Test
    void listFiltersByPartialNameAndTrimsInput() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.userListshow(model, "  taro ", null, null);
        assertEquals(List.of(taro, deleted), list(model));
        assertEquals("taro", model.get("userName"));
    }

    @Test
    void listFiltersByPermissionAndDeleteFlagTogether() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.userListshow(model, null, 0, true);
        assertEquals(List.of(deleted), list(model));

        controller.userListshow(model, null, 1, false);
        assertEquals(List.of(hanako), list(model));
    }

    @Test
    void listWithNoMatchIsEmptyNotAnError() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.userListshow(model, "zzz", null, null);
        assertTrue(list(model).isEmpty());
    }

    // ---- 削除確認 ----

    @Test
    void deleteCheckAsksForConfirmationForAnActiveOtherUser() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("user/user_delete_check", controller.userDeleteCheck(target(2, false), model));
        assertEquals("このユーザーを削除しますか?", model.get("flag"));
    }

    @Test
    void deleteCheckRefusesTheLoggedInUser() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("user/user_list", controller.userDeleteCheck(target(1, false), model));
        assertEquals("ログイン中のユーザーは操作できません", model.get("errorMessage"));
    }

    @Test
    void deleteCheckRefusesAnAlreadyDeletedUser() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("user/user_list", controller.userDeleteCheck(target(3, true), model));
        assertEquals("選択したユーザーは削除済みです", model.get("errorMessage"));
    }

    // ---- 復元確認 ----

    @Test
    void restoreCheckAsksForConfirmationForADeletedUser() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("user/user_delete_check", controller.userRestoreCheck(target(3, true), model));
        assertEquals("このユーザーを復元しますか?", model.get("flag"));
    }

    @Test
    void restoreCheckRefusesTheLoggedInUser() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("user/user_list", controller.userRestoreCheck(target(1, true), model));
        assertEquals("ログイン中のユーザーは操作できません", model.get("errorMessage"));
    }

    @Test
    void restoreCheckRefusesAnActiveUser() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("user/user_list", controller.userRestoreCheck(target(2, false), model));
        assertEquals("選択したユーザーはアクティブです", model.get("errorMessage"));
    }

    // ---- 完了（削除／復元の切り替え） ----

    @Test
    void completeDeletesAnActiveUser() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("user/user_delete_complete", controller.userDeleteComp(model, 2));
        assertTrue(hanako.getDeleteFlag());
        verify(repo).save(hanako);
        assertEquals("hanakoさんを削除しました", model.get("userName"));
    }

    @Test
    void completeRestoresADeletedUser() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.userDeleteComp(model, 3);
        assertFalse(deleted.getDeleteFlag());
        assertEquals("taroOldさんを復元しました", model.get("userName"));
    }

    @Test
    void completeForAnUnknownUserFailsWithoutSaving() {
        when(repo.findByUserId(99)).thenReturn(null);
        org.springframework.web.server.ResponseStatusException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () -> controller.userDeleteComp(new ExtendedModelMap(), 99));
        assertEquals(404, e.getStatusCode().value());
        verify(repo, never()).save(any());
    }
}
