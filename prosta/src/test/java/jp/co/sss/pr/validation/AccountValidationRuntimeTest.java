package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

class AccountValidationRuntimeTest extends ValidationSupport {
    @Test
    void registrationRejectsDuplicateThenCreatesHashedGeneralAccountAndAllowsLogoutLogin() throws Exception {
        long before = customers.count();
        mvc.perform(post("/regist/check").session(browser).param("_tab", java.util.UUID.randomUUID().toString())
                        .param("userName", user.getUserName()).param("userPass", "pass1234"))
                .andExpect(status().isOk()).andExpect(view().name("login/user_regist"))
                .andExpect(model().attribute("errMessage", "既に使用されているユーザー名です。"));
        assertEquals(before, customers.count());
        String name = "R" + java.util.UUID.randomUUID().toString().substring(0, 7);
        String redirect = mvc.perform(post("/regist/check").session(browser)
                        .param("_tab", java.util.UUID.randomUUID().toString())
                        .param("userName", name).param("userPass", "pass1234").param("permission", "1"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertNotNull(redirect);
        tab = org.springframework.web.util.UriComponentsBuilder.fromUriString(redirect)
                .build().getQueryParams().getFirst("_tab");
        assertNotNull(tab);
        var created = customers.findAllByUserName(name).get(0);
        assertEquals(before + 1, customers.count());
        assertEquals(0, created.getPermission());
        assertTrue(passwords.matches("pass1234", created.getUserPass()));
        assertNotEquals("pass1234", created.getUserPass());
        protectedPage("/mypage");
        mvc.perform(get("/logout").session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        assertNull(customers.findById(created.getUserId()).orElseThrow().getActiveLoginToken());
        browser = new org.springframework.mock.web.MockHttpSession();
        tab = login(created);
        protectedPage("/mypage");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(value = {
            "'',pass1234", "123456789,pass1234", "newname,abc",
            "newname,abcdefghijklmnopq", "newname,pass!",
            "newname,パスワード"
    })
    void invalidRegistrationBoundariesDoNotCreateOrAuthenticate(String name, String password) throws Exception {
        long before = customers.count();
        mvc.perform(post("/regist/check").session(browser).param("_tab", java.util.UUID.randomUUID().toString())
                        .param("userName", name).param("userPass", password))
                .andExpect(status().isOk()).andExpect(view().name("login/user_regist"))
                .andExpect(model().hasErrors());
        assertEquals(before, customers.count());
        mvc.perform(get("/mypage").session(browser).param("_tab", java.util.UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void duplicateAndInvalidNamesAtConfirmationAndCompletionDoNotChangeCredentials() throws Exception {
        tab = login(user);
        String originalName = user.getUserName();
        String originalPassword = customers.findById(user.getUserId()).orElseThrow().getUserPass();
        mvc.perform(post("/user/update/check").session(browser).param("_tab", tab)
                        .param("userId", user.getUserId().toString())
                        .param("userName", friend.getUserName()).param("userPass", "newpass123"))
                .andExpect(status().isOk()).andExpect(view().name("user/user_update_input"))
                .andExpect(model().attribute("err", "既に使用されているユーザー名です。"));
        mvc.perform(post("/user/update/check").session(browser).param("_tab", tab)
                        .param("userId", user.getUserId().toString()).param("userName", "123456789")
                        .param("userPass", "newpass123"))
                .andExpect(status().isOk()).andExpect(view().name("user/user_update_input"));
        mvc.perform(post("/user/update/check").session(browser).param("_tab", tab)
                        .param("userId", user.getUserId().toString())
                        .param("userName", originalName).param("userPass", "newpass123"))
                .andExpect(view().name("user/user_update_check"));
        mvc.perform(post("/user/update/complete").session(browser).param("_tab", tab)
                        .param("userId", user.getUserId().toString()).param("userName", friend.getUserName()))
                .andExpect(status().isOk()).andExpect(view().name("user/user_update_input"))
                .andExpect(model().attribute("err", "既に使用されているユーザー名です。"));
        assertEquals(originalName, customers.findById(user.getUserId()).orElseThrow().getUserName());
        assertEquals(originalPassword, customers.findById(user.getUserId()).orElseThrow().getUserPass());
        mvc.perform(post("/user/update/complete").session(browser).param("_tab", tab)
                        .param("userId", user.getUserId().toString()).param("userName", originalName)
                        .param("userPass", "tampered"))
                .andExpect(status().is3xxRedirection());
        assertEquals(originalPassword, customers.findById(user.getUserId()).orElseThrow().getUserPass());
        protectedPage("/mypage");
    }

    @Test
    void ordinaryUserCannotPromoteSelfAndCrossOriginUpdateCannotPersist() throws Exception {
        tab = login(user);
        mvc.perform(post("/user/update/check").session(browser).param("_tab", tab)
                        .param("userId", user.getUserId().toString()).param("userName", user.getUserName())
                        .param("userPass", "newpass123").param("permission", "1"))
                .andExpect(view().name("user/user_update_check"));
        mvc.perform(post("/user/update/complete").session(browser).param("_tab", tab)
                        .param("userId", user.getUserId().toString()).param("userName", user.getUserName())
                        .param("permission", "1"))
                .andExpect(status().isOk());
        assertEquals(0, customers.findById(user.getUserId()).orElseThrow().getPermission());
        String password = customers.findById(user.getUserId()).orElseThrow().getUserPass();
        mvc.perform(post("/user/update/check").session(browser).param("_tab", tab)
                        .header("Origin", "https://different.example")
                        .param("userId", user.getUserId().toString()).param("userName", "changed")
                        .param("userPass", "malicious123"))
                .andExpect(status().isForbidden());
        assertEquals(password, customers.findById(user.getUserId()).orElseThrow().getUserPass());
        protectedPage("/mypage");
    }
}
