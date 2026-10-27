package jp.co.sss.pr.mypage_function;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CategoryStudySummary;
import jp.co.sss.pr.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class LearningStatusControllerTest {

    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CustomerRepository customerRepository;

    private MockMvc mockMvc;
    private Customer customer;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new LearningStatusController(answerRepository, categoryRepository, customerRepository)).build();
        customer = new Customer();
        customer.setUserId(4);
        Mockito.lenient().when(customerRepository.getReferenceById(4)).thenReturn(customer);
        Category java = category(1, "Java");
        Category spring = category(2, "Spring");
        Mockito.lenient().when(categoryRepository.findAll()).thenReturn(List.of(java, spring));
        CustomerBean user = new CustomerBean();
        user.setUserId(4);
        session = new MockHttpSession();
        session.setAttribute("user", user);
    }

    @Test
    void buildsOverallAndCategoryAccuracyFromOwnActivity() throws Exception {
        when(answerRepository.summarizeByCategory(customer))
                .thenReturn(List.of(summary(1, "Java", 4, 1)));
        when(answerRepository.findRecentActivities(eq(customer), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/learning-status").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("mypage/learning_status"))
                .andExpect(model().attribute("totalAnswers", 4L))
                .andExpect(model().attribute("totalCorrect", 1L))
                .andExpect(model().attribute("totalIncorrect", 3L))
                .andExpect(model().attribute("overallAccuracy", 25))
                .andExpect(model().attributeExists("categoryProgress", "recentActivities"));
    }

    @Test
    void redirectsUnauthenticatedUsersWithoutLoadingTheirActivity() throws Exception {
        mockMvc.perform(get("/learning-status"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));

        verify(answerRepository, never()).summarizeByCategory(any());
        verify(answerRepository, never()).findRecentActivities(any(), any());
    }

    private Category category(int id, String name) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryName(name);
        return category;
    }

    private CategoryStudySummary summary(int id, String name, long attempts, long correct) {
        return new CategoryStudySummary() {
            @Override
            public Integer getCategoryId() { return id; }
            @Override
            public String getCategoryName() { return name; }
            @Override
            public Long getAnswerCount() { return attempts; }
            @Override
            public Long getCorrectCount() { return correct; }
        };
    }
}
