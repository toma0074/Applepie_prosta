package jp.co.sss.pr.mypage_function;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.StudyPlan;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.StudyPlanRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;

@WebMvcTest(StudyCalendarController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudyCalendarTemplateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnswerRepository answerRepository;
    @MockBean
    private StudyPlanRepository studyPlanRepository;
    @MockBean
    private CustomerRepository customerRepository;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private FriendConnectionRepository friendConnectionRepository;
    @MockBean
    private UserAchievementRepository userAchievementRepository;

    @Test
    void rendersMonthCalendarAndPlanForm() throws Exception {
        Customer customer = new Customer();
        customer.setUserId(1);
        when(customerRepository.getReferenceById(1)).thenReturn(customer);
        when(answerRepository.summarizeDailyStudy(
                customer, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of());
        StudyPlan plan = new StudyPlan();
        plan.setStudyPlanId(3);
        plan.setCustomer(customer);
        plan.setPlanDate(LocalDate.of(2026, 3, 12));
        plan.setTitle("Java復習");
        plan.setDetails("配列を復習する");
        when(studyPlanRepository.findAllByCustomerAndPlanDateBetweenOrderByPlanDateAscStudyPlanIdAsc(
                customer, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(plan));
        when(studyPlanRepository.findAllByCustomerAndPlanDateOrderByStudyPlanIdAsc(
                customer, LocalDate.of(2026, 3, 12)))
                .thenReturn(List.of(plan));

        CustomerBean user = new CustomerBean();
        user.setUserId(1);
        user.setPermission(0);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        mockMvc.perform(get("/study-calendar")
                        .param("month", "2026-03")
                        .param("date", "2026-03-12")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("学習カレンダー・予定表")))
                .andExpect(content().string(Matchers.containsString("2026-03")))
                .andExpect(content().string(Matchers.containsString("Javaの問題を10問解く")))
                .andExpect(content().string(Matchers.containsString("Java復習")))
                .andExpect(content().string(Matchers.containsString("配列を復習する")))
                .andExpect(content().string(Matchers.containsString("/study-calendar/plans/3/delete")))
                .andExpect(content().string(Matchers.containsString("予定を登録")));
    }
}
