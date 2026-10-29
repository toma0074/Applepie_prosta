package jp.co.sss.pr.question_function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.FavoriteRepository;
import jp.co.sss.pr.repository.QuestionRepository;

@WebMvcTest(AdminQuestionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionRepository questionRepository;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private AnswerRepository answerRepository;
    @MockBean
    private FavoriteRepository favoriteRepository;
    @MockBean
    private jp.co.sss.pr.repository.CustomerRepository customerRepository;
    @MockBean
    private jp.co.sss.pr.repository.FriendConnectionRepository friendConnectionRepository;

    private MockHttpSession adminSession() {
        CustomerBean user = new CustomerBean();
        user.setUserId(1);
        user.setUserName("admin");
        user.setPermission(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }

    private Category category() {
        Category c = new Category();
        c.setCategoryId(1);
        c.setCategoryName("Java");
        return c;
    }

    private Question question() {
        Question q = new Question();
        q.setQuestionId(10);
        q.setCategory(category());
        q.setDifficulty(2);
        q.setQuestionText("sample question text");
        q.setOptionA("a");
        q.setOptionB("b");
        q.setOptionC("c");
        q.setCorrectOption(2);
        return q;
    }

    @Test
    void listRendersRows() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of(category()));
        when(questionRepository.search(any(), any(), any(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(question())));

        mockMvc.perform(get("/admin/questions").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("sample question text")))
                .andExpect(content().string(Matchers.containsString("/admin/questions/10/edit")));
    }

    @Test
    void pageSelectorKeepsAllQuestionSearchFilters() throws Exception {
        when(questionRepository.search(any(), any(), any(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(question()), PageRequest.of(1, 10), 30));

        mockMvc.perform(get("/admin/questions").session(adminSession())
                .param("categoryId", "1").param("difficulty", "2")
                .param("keyword", "sample").param("noExplanation", "true").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("data-page-navigation")))
                .andExpect(content().string(Matchers.containsString("page=1\" selected=\"selected\">2</option>")))
                .andExpect(content().string(Matchers.containsString(
                        "data-page-url=\"/admin/questions?categoryId=1&amp;difficulty=2&amp;keyword=sample&amp;noExplanation=true&amp;page=2\"")));
    }

    @Test
    void newAndEditFormsRender() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of(category()));
        when(questionRepository.findById(10)).thenReturn(Optional.of(question()));

        mockMvc.perform(get("/admin/questions/new").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question_form"));
        mockMvc.perform(get("/admin/questions/10/edit").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("sample question text")));
    }

    @Test
    void saveWithInvalidInputRedisplaysFormWithoutSaving() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of(category()));

        mockMvc.perform(post("/admin/questions/save").session(adminSession())
                .param("questionText", "x"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question_form"));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void saveCreatesQuestionAndRedirects() throws Exception {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category()));

        mockMvc.perform(post("/admin/questions/save").session(adminSession())
                .param("category", "1").param("difficulty", "2")
                .param("questionText", "valid question text")
                .param("optionA", "a").param("optionB", "b").param("optionC", "c")
                .param("correctOption", "3").param("explanation", "  because  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/questions"));
        verify(questionRepository).save(argThat(
                (Question q) -> "because".equals(q.getExplanation()) && q.getCorrectOption() == 3));
    }

    @Test
    void saveWhenSequenceIsOutOfSyncShowsValidationError() throws Exception {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category()));
        doThrow(new DataIntegrityViolationException("ORA-00001")).when(questionRepository).flush();

        mockMvc.perform(post("/admin/questions/save").session(adminSession())
                .param("category", "1").param("difficulty", "2")
                .param("questionText", "valid question text")
                .param("optionA", "a").param("optionB", "b").param("optionC", "c")
                .param("correctOption", "3").param("explanation", "because"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question_form"))
                .andExpect(content().string(Matchers.containsString("SEQ_QUESTION")));
    }

    @Test
    void deleteIsRefusedWhenAnswersExist() throws Exception {
        Question q = question();
        when(questionRepository.findById(10)).thenReturn(Optional.of(q));
        when(answerRepository.existsByQuestion(q)).thenReturn(true);

        mockMvc.perform(post("/admin/questions/10/delete").session(adminSession()))
                .andExpect(redirectedUrl("/admin/questions"))
                .andExpect(flash().attributeExists("error"));
        verify(questionRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesUnusedQuestion() throws Exception {
        Question q = question();
        when(questionRepository.findById(10)).thenReturn(Optional.of(q));

        mockMvc.perform(post("/admin/questions/10/delete").session(adminSession()))
                .andExpect(flash().attributeExists("message"));
        verify(questionRepository).delete(q);
    }
}
