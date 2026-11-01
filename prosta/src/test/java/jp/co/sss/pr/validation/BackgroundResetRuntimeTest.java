package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BackgroundResetRuntimeTest extends ValidationSupport {
    @ParameterizedTest(name = "permission={0}, background={1}")
    @CsvSource({"0,color", "0,image", "1,color", "1,image"})
    void resetClearsPersistedSettingsAndSurvivesLogoutAndLogin(int permission, String mode) throws Exception {
        var actor = permission == 1 ? admin : user;
        tab = login(actor);
        var request = "color".equals(mode)
                ? post("/background/changeColor").param("backgroundColor", "#123456")
                : post("/background/change").param("background", "bg-sky");
        mvc.perform(request.session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        assertNotNull(customers.findById(actor.getUserId()).orElseThrow().getBackgroundClass());
        mvc.perform(post("/background/default").session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        var saved = customers.findById(actor.getUserId()).orElseThrow();
        assertNull(saved.getBackgroundClass());
        assertNull(saved.getBackgroundColor());
        mvc.perform(get("/logout").session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        browser = new org.springframework.mock.web.MockHttpSession();
        tab = login(actor);
        String html = mvc.perform(get("/mypage").session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("class=\"bg-default\""));
        assertFalse(html.contains("background-color:#123456"));
        assertFalse(html.contains("class=\"bg-sky\""));
    }
}
