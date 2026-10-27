package jp.co.sss.pr.mypage_function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CategoryStudySummary;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.RecentStudyActivity;
import jp.co.sss.pr.repository.StudyPlanRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;

@WebMvcTest(LearningStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class LearningStatusTemplateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnswerRepository answerRepository;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private CustomerRepository customerRepository;
    @MockBean
    private FriendConnectionRepository friendConnectionRepository;
    @MockBean
    private UserAchievementRepository userAchievementRepository;
    @MockBean
    private StudyPlanRepository studyPlanRepository;

    @Test
    void rendersWeaknessChartAndRecentActivity() throws Exception {
        Customer customer = new Customer();
        customer.setUserId(1);
        when(customerRepository.getReferenceById(1)).thenReturn(customer);
        Category java = new Category();
        java.setCategoryId(1);
        java.setCategoryName("Java");
        Category spring = new Category();
        spring.setCategoryId(2);
        spring.setCategoryName("Spring");
        when(categoryRepository.findAll()).thenReturn(List.of(java, spring));
        CategoryStudySummary javaSummary = summary(1, "Java", 4, 1);
        when(answerRepository.summarizeByCategory(customer))
                .thenReturn(List.of(javaSummary));
        RecentStudyActivity activity = org.mockito.Mockito.mock(RecentStudyActivity.class);
        when(activity.getStudyDate()).thenReturn(LocalDate.of(2026, 10, 27));
        when(activity.getCategoryName()).thenReturn("Java");
        when(activity.getQuestionText()).thenReturn("int型の特徴はどれですか？");
        when(activity.getCorrectFlag()).thenReturn(false);
        when(answerRepository.findRecentActivities(eq(customer), any()))
                .thenReturn(new PageImpl<>(List.of(activity)));

        CustomerBean user = new CustomerBean();
        user.setUserId(1);
        user.setPermission(0);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        mockMvc.perform(get("/learning-status").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("学習状況")))
                .andExpect(content().string(Matchers.containsString("カテゴリ別の弱点グラフ")))
                .andExpect(content().string(Matchers.containsString("25%")))
                .andExpect(content().string(Matchers.containsString("復習の目安")))
                .andExpect(content().string(Matchers.containsString("最近の学習活動")))
                .andExpect(content().string(Matchers.containsString("int型の特徴はどれですか？")))
                .andExpect(content().string(Matchers.containsString("不正解")));
    }

    private CategoryStudySummary summary(int id, String name, long attempts, long correct) {
        CategoryStudySummary summary = org.mockito.Mockito.mock(CategoryStudySummary.class);
        when(summary.getCategoryId()).thenReturn(id);
        when(summary.getCategoryName()).thenReturn(name);
        when(summary.getAnswerCount()).thenReturn(attempts);
        when(summary.getCorrectCount()).thenReturn(correct);
        return summary;
    }
}
