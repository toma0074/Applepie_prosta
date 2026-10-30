package jp.co.sss.pr.mypage_function;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
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
    void showsPendingRequestCountBadgeInSidebar() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());
        Customer customer = new Customer();
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(friendConnectionRepository.countIncomingPending(customer)).thenReturn(3L);
        CustomerBean user = new CustomerBean();
        user.setUserId(1);
        user.setPermission(0);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        mockMvc.perform(get("/friends").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("notify-badge")));
    }

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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("カテゴリ別ランキング")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("const apiBase")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("フレンド")));
    }
}
