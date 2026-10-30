package jp.co.sss.pr.login_function;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import jp.co.sss.pr.question_function.QuestionController;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.service.LevelService;
import jp.co.sss.pr.service.PasswordService;

@WebMvcTest({LoginController.class, QuestionController.class})
@AutoConfigureMockMvc(addFilters = false)
class LogoutHeaderTemplateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerRepository customerRepository;
    @MockBean
    private AnswerRepository answerRepository;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private FriendConnectionRepository friendConnectionRepository;
    @MockBean
    private QuestionRepository questionRepository;
    @MockBean
    private LevelService levelService;
    @MockBean
    private PasswordService passwordService;

    private MockHttpSession loggedInSession() {
        CustomerBean user = new CustomerBean();
        user.setUserId(1);
        user.setUserName("taro");
        user.setPermission(0);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }

    @Test
    void ordinaryPageHeaderLeadsToSummary() throws Exception {
        mockMvc.perform(get("/prosta/question").contextPath("/prosta").session(loggedInSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<a class=\"btn\" href=\"/prosta/logout/analyze\">")));
    }

    @Test
    void summaryHeaderLogsOutAndOrdinaryPageStillLeadsToSummary() throws Exception {
        MockHttpSession session = loggedInSession();
        Customer customer = new Customer();
        customer.setUserId(1);
        when(customerRepository.getReferenceById(1)).thenReturn(customer);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        mockMvc.perform(get("/prosta/logout/analyze").contextPath("/prosta").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<a class=\"btn\" href=\"/prosta/logout\">")));
        assertFalse(session.isInvalid());

        mockMvc.perform(get("/prosta/question").contextPath("/prosta").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<a class=\"btn\" href=\"/prosta/logout/analyze\">")));

        mockMvc.perform(get("/prosta/logout").contextPath("/prosta").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prosta/"));
        assertTrue(session.isInvalid());
        verify(customerRepository).save(customer);
    }

    @Test
    void summaryWithoutLoginRedirectsToTop() throws Exception {
        mockMvc.perform(get("/prosta/logout/analyze").contextPath("/prosta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prosta/"));
    }
}
