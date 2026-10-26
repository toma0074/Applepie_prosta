package jp.co.sss.pr.test_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class TestControllerTimerTest {

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
        customer.setUserId(1);
        lenient().when(session.getAttribute("user")).thenReturn(bean);
        lenient().when(customerRepository.getReferenceById(1)).thenReturn(customer);
    }

    @Test
    void weakStartSetsTheLimitTo25SecondsPerQuestion() throws Exception {
        lenient().when(session.getAttribute(TestController.TIMER_START)).thenReturn(System.currentTimeMillis());
        lenient().when(session.getAttribute(TestController.TIMER_LIMIT)).thenReturn(50_000L);
        lenient().when(weakQuestionService.pick(any(), eq(15))).thenReturn(List.of(
                new WeakQuestion(question(10), 3, 0), new WeakQuestion(question(11), 2, 1)));

        mockMvc.perform(get("/test/weak/start").param("timer", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("timerRemainingMillis"))
                .andExpect(model().attribute("timerTotalMillis", 50_000L));

        verify(session).setAttribute(eq(TestController.TIMER_LIMIT), eq(50_000L));
    }

    @Test
    void weakStartWithoutTimerOptionHasNoTimer() throws Exception {
        lenient().when(weakQuestionService.pick(any(), eq(15))).thenReturn(List.of(
                new WeakQuestion(question(10), 3, 0)));

        mockMvc.perform(get("/test/weak/start"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("timerRemainingMillis"));

        verify(session).removeAttribute(TestController.TIMER_START);
        verify(session, never()).setAttribute(eq(TestController.TIMER_START), any());
    }

    @Test
    void reviewStartHasNoTimer() throws Exception {
        lenient().when(weakQuestionService.pickRecentMistakes(any(), eq(15)))
                .thenReturn(new java.util.ArrayList<>(List.of(question(10))));

        mockMvc.perform(get("/test/review/start"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("timerRemainingMillis"));

        verify(session).removeAttribute(TestController.TIMER_START);
        verify(session, never()).setAttribute(eq(TestController.TIMER_START), any());
    }

    @Test
    void timeUpWithoutATimerRedirectsToMyPage() throws Exception {
        mockMvc.perform(post("/test/timeup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/mypage"));
    }

    @Test
    void timeUpScoresUnansweredQuestionsAsDontKnow() throws Exception {
        lenient().when(session.getAttribute(TestController.TIMER_START)).thenReturn(System.currentTimeMillis());
        lenient().when(session.getAttribute("question")).thenReturn(List.of(question(10), question(11)));
        AnswerForm answered = new AnswerForm();
        answered.setAnsOption(1);
        answered.setCorrectOption(1);
        lenient().when(session.getAttribute("ans")).thenReturn(List.of(answered));

        mockMvc.perform(post("/test/timeup")
                .param("results[0].questionId", "10").param("results[0].correctOption", "1")
                .param("results[0].ansOption", "1")
                .param("results[1].questionId", "11").param("results[1].correctOption", "2"))
                .andExpect(status().is3xxRedirection());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnswerForm>> saved = ArgumentCaptor.forClass(List.class);
        verify(session).setAttribute(eq("ans"), saved.capture());
        assertEquals(1, saved.getValue().get(0).getAnsOption());
        assertEquals(4, saved.getValue().get(1).getAnsOption());
    }

    private Question question(int id) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setQuestionText("Q" + id);
        return q;
    }
}
