package jp.co.sss.pr.mypage_function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.StudyPlan;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.DailyStudySummary;
import jp.co.sss.pr.repository.StudyPlanRepository;

@ExtendWith(MockitoExtension.class)
class StudyCalendarControllerTest {

    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private StudyPlanRepository studyPlanRepository;
    @Mock
    private CustomerRepository customerRepository;

    private MockMvc mockMvc;
    private Customer customer;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new StudyCalendarController(answerRepository, studyPlanRepository, customerRepository)).build();
        customer = new Customer();
        customer.setUserId(1);
        Mockito.lenient().when(customerRepository.getReferenceById(1)).thenReturn(customer);
        CustomerBean user = new CustomerBean();
        user.setUserId(1);
        session = new MockHttpSession();
        session.setAttribute("user", user);
    }

    @Test
    void rendersSelectedMonthWithDailyActivityAndPlans() throws Exception {
        when(answerRepository.summarizeDailyStudy(customer,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(summary(LocalDate.of(2026, 3, 12), 4, 3)));
        StudyPlan plan = new StudyPlan();
        plan.setPlanDate(LocalDate.of(2026, 3, 12));
        plan.setTitle("Java復習");
        when(studyPlanRepository.findAllByCustomerAndPlanDateBetweenOrderByPlanDateAscStudyPlanIdAsc(
                customer, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(plan));
        when(studyPlanRepository.findAllByCustomerAndPlanDateOrderByStudyPlanIdAsc(
                customer, LocalDate.of(2026, 3, 12)))
                .thenReturn(List.of(plan));

        mockMvc.perform(get("/study-calendar")
                        .param("month", "2026-03")
                        .param("date", "2026-03-12")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("mypage/study_calendar"))
                .andExpect(model().attribute("monthLabel", "2026-03"))
                .andExpect(model().attribute("answerCount", 4L))
                .andExpect(model().attribute("correctCount", 3L))
                .andExpect(model().attribute("incorrectCount", 1L))
                .andExpect(model().attributeExists("weeks", "selectedPlans"));
    }

    @Test
    void redirectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/study-calendar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));

        verify(answerRepository, never()).summarizeDailyStudy(any(), any(), any());
    }

    @Test
    void createsOwnedStudyPlanAndRedirectsToItsDate() throws Exception {
        mockMvc.perform(post("/study-calendar/plans")
                        .session(session)
                        .param("planDate", "2026-03-12")
                        .param("title", "  Java復習  ")
                        .param("details", "  配列を復習  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/study-calendar?month=2026-03&date=2026-03-12"))
                .andExpect(flash().attribute("planMessage", "学習予定を登録しました。"));

        verify(studyPlanRepository).save(org.mockito.ArgumentMatchers.argThat(plan ->
                plan.getCustomer() == customer
                        && plan.getPlanDate().equals(LocalDate.of(2026, 3, 12))
                        && plan.getTitle().equals("Java復習")
                        && plan.getDetails().equals("配列を復習")));
    }

    @Test
    void rejectsBlankPlanTitleWithoutSaving() throws Exception {
        mockMvc.perform(post("/study-calendar/plans")
                        .session(session)
                        .param("planDate", "2026-03-12")
                        .param("title", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("planError", "予定のタイトルを入力してください。"));

        verify(studyPlanRepository, never()).save(any(StudyPlan.class));
    }

    @Test
    void doesNotAllowEditingAnotherUsersPlan() {
        when(studyPlanRepository.findByStudyPlanIdAndCustomer(3, customer)).thenReturn(Optional.empty());

        try {
            mockMvc.perform(post("/study-calendar/plans/3")
                        .session(session)
                        .param("planDate", "2026-03-12")
                        .param("title", "侵入"))
                    .andExpect(status().isNotFound());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }

        verify(studyPlanRepository, never()).save(any(StudyPlan.class));
    }

    private DailyStudySummary summary(LocalDate date, long answers, long correct) {
        return new DailyStudySummary() {
            @Override
            public LocalDate getStudyDate() {
                return date;
            }

            @Override
            public Long getAnswerCount() {
                return answers;
            }

            @Override
            public Long getCorrectCount() {
                return correct;
            }
        };
    }
}
