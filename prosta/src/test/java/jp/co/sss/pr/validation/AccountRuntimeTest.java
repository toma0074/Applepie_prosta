package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

class AccountRuntimeTest extends ValidationSupport {
    @Test
    void loginUpdateReadBackAndLogoutWithRealDatabase() throws Exception {
        mvc.perform(get("/mypage").session(browser).param("_tab", java.util.UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection());
        tab = login(user);
        protectedPage("/mypage");
        protectedPage("/user/update/input");
        String name = user.getUserName() + "改";
        mvc.perform(post("/user/update/check").session(browser).param("_tab", tab)
                        .param("userId", user.getUserId().toString()).param("userName", name)
                        .param("userPass", "newpass123"))
                .andExpect(status().isOk()).andExpect(view().name("user/user_update_check"));
        mvc.perform(post("/user/update/complete").session(browser).param("_tab", tab)
                        .param("userId", user.getUserId().toString()).param("userName", name))
                .andExpect(status().isOk()).andExpect(view().name("user/user_update_complete"));
        var saved = customers.findById(user.getUserId()).orElseThrow();
        assertEquals(name, saved.getUserName());
        assertTrue(passwords.matches("newpass123", saved.getUserPass()));
        protectedPage("/logout/analyze");
        mvc.perform(get("/logout").session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        assertNull(customers.findById(user.getUserId()).orElseThrow().getActiveLoginToken());
    }
}
