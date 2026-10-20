package jp.co.sss.pr.mypage_function;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class FriendPageControllerTest {

    @Mock
    private CategoryRepository categoryRepository;

    private MockMvc mockMvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new FriendPageController(categoryRepository)).build();
        CustomerBean user = new CustomerBean();
        user.setUserId(1);
        user.setPermission(1);
        session = new MockHttpSession();
        session.setAttribute("user", user);
    }

    @Test
    void servesFriendPageWithCategoriesForTheRankingForm() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/friends").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("friends/index"))
                .andExpect(model().attributeExists("categoryList"));

        verify(categoryRepository).findAll();
    }

    @Test
    void redirectsToLoginWhenSessionIsMissing() throws Exception {
        mockMvc.perform(get("/friends"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));
    }

    @Test
    void redirectsAdministratorsAwayFromFriendPage() throws Exception {
        CustomerBean admin = new CustomerBean();
        admin.setUserId(2);
        admin.setPermission(2);
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("user", admin);

        mockMvc.perform(get("/friends").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/mypage"));
    }
}
