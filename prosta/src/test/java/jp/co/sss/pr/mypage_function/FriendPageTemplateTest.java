package jp.co.sss.pr.mypage_function;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;

import static org.mockito.Mockito.when;

@WebMvcTest(FriendPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class FriendPageTemplateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private FriendConnectionRepository friendConnectionRepository;

    @Test
    void rendersFriendPageThroughThymeleafLayout() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());
        CustomerBean user = new CustomerBean();
        user.setUserId(1);
        user.setPermission(0);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        mockMvc.perform(get("/friends").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ランキング")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"friend-feed-modal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"friend-feed-character\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"friend-character-1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"friend-character-10\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"friend-character-25\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"friend-character-50\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"friend-character-75\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"friend-character-100\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/images/suta5.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("フィードを見る")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-labelledby=\"friend-feed-title\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("const apiBase")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("フレンド")));
    }
}
