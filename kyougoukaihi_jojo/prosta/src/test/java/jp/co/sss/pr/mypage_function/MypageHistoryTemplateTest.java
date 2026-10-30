package jp.co.sss.pr.mypage_function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;
import jp.co.sss.pr.service.LevelService;

@WebMvcTest(MypageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MypageHistoryTemplateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnswerRepository answerRepository;
    @MockBean
    private CustomerRepository customerRepository;
    @MockBean
    private LevelService levelService;
    @MockBean
    private UserAchievementRepository userAchievementRepository;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private FriendConnectionRepository friendConnectionRepository;

    @Test
    void questionTextCarriesAllOptionsAndTheCorrectAnswerForTheModal() throws Exception {
        Customer customer = new Customer();
        customer.setUserId(1);
        when(customerRepository.getReferenceById(1)).thenReturn(customer);

        Category category = new Category();
        category.setCategoryId(1);
        category.setCategoryName("Java");
        Question question = new Question();
        question.setCategory(category);
        question.setQuestionText("整数を表す型は？");
        question.setOptionA("double");
        question.setOptionB("int");
        question.setOptionC("char");
        question.setCorrectOption(2);
        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setAnsOption(3);
        answer.setCorrectOption(2);
        when(answerRepository.searchHistory(eq(customer), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(answer)));
        when(userAchievementRepository.findByCustomerOrderByUnlockedDateDesc(customer)).thenReturn(List.of());

        CustomerBean bean = new CustomerBean();
        bean.setUserId(1);
        bean.setPermission(0);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", bean);

        mockMvc.perform(get("/mypage").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("class=\"question-link\"")))
                .andExpect(content().string(Matchers.containsString("data-option-a=\"double\"")))
                .andExpect(content().string(Matchers.containsString("data-option-b=\"int\"")))
                .andExpect(content().string(Matchers.containsString("data-option-c=\"char\"")))
                .andExpect(content().string(Matchers.containsString("data-correct=\"2\"")))
                .andExpect(content().string(Matchers.containsString("data-chosen=\"3\"")))
                .andExpect(content().string(Matchers.containsString("name=\"fromDate\"")))
                .andExpect(content().string(Matchers.containsString("id=\"question-modal\"")));
    }
}
