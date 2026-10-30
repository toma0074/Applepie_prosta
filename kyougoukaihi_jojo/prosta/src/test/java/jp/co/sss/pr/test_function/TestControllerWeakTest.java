package jp.co.sss.pr.test_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.AnswerForm;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.service.LevelService;
import jp.co.sss.pr.service.WeakQuestionService;
import jp.co.sss.pr.service.WeakQuestionService.WeakQuestion;

@ExtendWith(MockitoExtension.class)
class TestControllerWeakTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private LevelService levelService;
    @Mock
    private WeakQuestionService weakQuestionService;
    @Mock
    private HttpSession session;

    @InjectMocks
    private TestController controller;

    private MockMvc mockMvc;
    private final Customer customer = new Customer();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        CustomerBean bean = new CustomerBean();
        bean.setUserId(1);
        when(session.getAttribute("user")).thenReturn(bean);
        when(customerRepository.getReferenceById(1)).thenReturn(customer);
    }

    @Test
    void weakPageShowsWeakQuestionsForTheLoggedInUser() throws Exception {
        List<WeakQuestion> weak = List.of(new WeakQuestion(question(10, "Q1"), 3, 1));
        when(weakQuestionService.pick(customer, 15)).thenReturn(weak);

        mockMvc.perform(get("/test/weak"))
                .andExpect(status().isOk())
                .andExpect(view().name("test/select_weak"))
                .andExpect(model().attribute("weakList", weak))
                .andExpect(model().attribute("minAttempts", WeakQuestionService.MIN_ATTEMPTS));
    }

    @Test
    void startPutsWeakQuestionsAndBlankAnswersIntoTheSession() throws Exception {
        Question q1 = question(10, "Q1");
        Question q2 = question(11, "Q2");
        when(weakQuestionService.pick(customer, 15)).thenReturn(List.of(
                new WeakQuestion(q1, 3, 0), new WeakQuestion(q2, 2, 1)));

        mockMvc.perform(get("/test/weak/start"))
                .andExpect(status().isOk())
                .andExpect(view().name("test/show_test"))
                .andExpect(model().attributeExists("answerListForm"));

        verify(session).setAttribute("question", List.of(q1, q2));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnswerForm>> answers = ArgumentCaptor.forClass(List.class);
        verify(session).setAttribute(eq("ans"), answers.capture());
        assertEquals(2, answers.getValue().size());
        assertEquals(0, answers.getValue().get(0).getAnsOption());
    }

    @Test
    void startRedirectsBackWhenThereAreNoWeakQuestions() throws Exception {
        when(weakQuestionService.pick(customer, 15)).thenReturn(List.of());

        mockMvc.perform(get("/test/weak/start"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/test/weak"));

        verify(session, never()).setAttribute(eq("question"), any());
    }

    private Question question(int id, String text) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setQuestionText(text);
        return q;
    }
}
