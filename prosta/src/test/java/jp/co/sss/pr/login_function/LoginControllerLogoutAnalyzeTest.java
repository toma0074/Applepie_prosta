package jp.co.sss.pr.login_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryStudySummary;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.DailyStudySummary;

@ExtendWith(MockitoExtension.class)
class LoginControllerLogoutAnalyzeTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private HttpSession session;

    @InjectMocks
    private LoginController loginController;

    @Test
    void redirectsToTopWhenNotLoggedIn() {
        when(session.getAttribute("user")).thenReturn(null);

        assertEquals("redirect:/", loginController.logoutAnalyze(new ExtendedModelMap()));
    }

    @Test
    void summarizesTodayAndWeakestCategory() {
        CustomerBean bean = new CustomerBean();
        bean.setUserId(1);
        Customer customer = new Customer();
        when(session.getAttribute("user")).thenReturn(bean);
        when(customerRepository.getReferenceById(1)).thenReturn(customer);

        DailyStudySummary daily = mock(DailyStudySummary.class);
        when(daily.getAnswerCount()).thenReturn(4L);
        when(daily.getCorrectCount()).thenReturn(3L);
        when(answerRepository.summarizeDailyStudy(any(), any(), any())).thenReturn(List.of(daily));

        CategoryStudySummary java = category("Java", 10, 9);
        CategoryStudySummary sql = category("SQL", 10, 4);
        when(answerRepository.summarizeByCategory(customer)).thenReturn(List.of(java, sql));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = loginController.logoutAnalyze(model);

        assertEquals("logout/logout_analyze", view);
        assertEquals(4L, model.get("todayAnswers"));
        assertEquals(75L, model.get("todayAccuracy"));
        assertEquals(65L, model.get("totalAccuracy"));
        assertEquals("SQL", model.get("weakCategory"));
        assertEquals(40L, model.get("weakAccuracy"));
    }

    private CategoryStudySummary category(String name, long answers, long correct) {
        CategoryStudySummary s = mock(CategoryStudySummary.class);
        org.mockito.Mockito.lenient().when(s.getCategoryName()).thenReturn(name);
        org.mockito.Mockito.lenient().when(s.getAnswerCount()).thenReturn(answers);
        org.mockito.Mockito.lenient().when(s.getCorrectCount()).thenReturn(correct);
        return s;
    }
}
