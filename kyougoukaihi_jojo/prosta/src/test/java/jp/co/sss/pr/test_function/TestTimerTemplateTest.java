package jp.co.sss.pr.test_function;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.service.LevelService;
import jp.co.sss.pr.service.WeakQuestionService;
import jp.co.sss.pr.service.WeakQuestionService.WeakQuestion;

@WebMvcTest(TestController.class)
@AutoConfigureMockMvc(addFilters = false)
class TestTimerTemplateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionRepository questionRepository;
    @MockBean
    private AnswerRepository answerRepository;
    @MockBean
    private CustomerRepository customerRepository;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private LevelService levelService;
    @MockBean
    private WeakQuestionService weakQuestionService;
    @MockBean
    private FriendConnectionRepository friendConnectionRepository;

    private MockHttpSession loggedIn() {
        CustomerBean user = new CustomerBean();
        user.setUserId(1);
        user.setPermission(0);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }

    @Test
    void weakTestShowsTheTimerWith25SecondsPerQuestion() throws Exception {
        Customer customer = new Customer();
        when(customerRepository.getReferenceById(1)).thenReturn(customer);
        Question q = new Question();
        q.setQuestionId(10);
        q.setQuestionText("Q1");
        Category c = new Category();
        c.setCategoryId(1);
        q.setCategory(c);
        when(weakQuestionService.pick(any(), eq(15))).thenReturn(List.of(new WeakQuestion(q, 3, 0)));

        mockMvc.perform(get("/test/weak/start").param("timer", "true").session(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"test-timer\"")))
                .andExpect(content().string(containsString("data-total=\"25000\"")))
                .andExpect(content().string(containsString("id=\"answer-form\"")));
    }

    @Test
    void reviewTestHasNoTimer() throws Exception {
        Customer customer = new Customer();
        when(customerRepository.getReferenceById(1)).thenReturn(customer);
        Question q = new Question();
        q.setQuestionId(10);
        q.setQuestionText("Q1");
        Category c = new Category();
        c.setCategoryId(1);
        q.setCategory(c);
        when(weakQuestionService.pickRecentMistakes(any(), eq(15)))
                .thenReturn(new java.util.ArrayList<>(List.of(q)));

        mockMvc.perform(get("/test/review/start").session(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"test-timer\""))));
    }
}
