package jp.co.sss.pr.login_function;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.service.PasswordService;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TabSessionIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private CustomerRepository customers;
    @Autowired private PasswordService passwords;
    @Autowired private AnswerRepository answers;
    @Autowired private CategoryRepository categories;
    @Autowired private QuestionRepository questions;

    private String login(MockHttpSession browser, String name) throws Exception {
        Customer customer = new Customer();
        customer.setUserName(name);
        customer.setUserPass(passwords.encode("pass1234"));
        customer.setPermission(0);
        customer.setDeleteFlag(false);
        customers.saveAndFlush(customer);
        return loginExisting(browser, name);
    }

    private String loginExisting(MockHttpSession browser, String name) throws Exception {
        String oldToken = UUID.randomUUID().toString();
        String redirect = mvc.perform(post("/login").session(browser)
                        .param("_tab", oldToken).param("userName", name).param("userPass", "pass1234"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertNotNull(redirect);
        String token = UriComponentsBuilder.fromUriString(redirect).build().getQueryParams().getFirst("_tab");
        assertNotNull(token);
        assertNotEquals(oldToken, token);
        return token;
    }

    @Test
    void newestLoginInvalidatesOldTabIncludingLogoutAndUpdates() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        String oldTab = login(browser, "single");
        String newest = loginExisting(browser, "single");
        mvc.perform(post("/background/changeColor").session(browser).param("_tab", oldTab)
                        .param("backgroundColor", "#ABCDEF"))
                .andExpect(status().is3xxRedirection());
        assertNull(customers.findByUserName("single").getBackgroundColor());
        mvc.perform(get("/logout").session(browser).param("_tab", oldTab))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/mypage").session(browser).param("_tab", newest)).andExpect(status().isOk());
        assertNotNull(customers.findByUserName("single").getActiveLoginToken());
    }

    @Test
    void anotherDeviceRevokesHeartbeatAndOldLogoutCannotRevokeNewLogin() throws Exception {
        MockHttpSession firstDevice = new MockHttpSession();
        MockHttpSession secondDevice = new MockHttpSession();
        String oldTab = login(firstDevice, "device");
        String newest = loginExisting(secondDevice, "device");
        String token = customers.findByUserName("device").getActiveLoginToken();
        mvc.perform(get("/logout").session(firstDevice).param("_tab", oldTab))
                .andExpect(status().is3xxRedirection());
        assertEquals(token, customers.findByUserName("device").getActiveLoginToken());
        mvc.perform(get("/mypage").session(secondDevice).param("_tab", newest)).andExpect(status().isOk());

        String third = loginExisting(firstDevice, "device");
        mvc.perform(post("/api/login-status/heartbeat").session(secondDevice).param("_tab", newest))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Prosta-Login-Expired", "true"));
        mvc.perform(get("/mypage").session(firstDevice).param("_tab", third)).andExpect(status().isOk());
        mvc.perform(get("/logout").session(firstDevice).param("_tab", third))
                .andExpect(status().is3xxRedirection());
        assertNull(customers.findByUserName("device").getActiveLoginToken());
    }

    @Test
    void failedLoginDoesNotRevokeExistingLogin() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        String valid = login(browser, "failure");
        String token = customers.findByUserName("failure").getActiveLoginToken();
        mvc.perform(post("/login").session(new MockHttpSession()).param("_tab", UUID.randomUUID().toString())
                        .param("userName", "failure").param("userPass", "incorrect"))
                .andExpect(status().isOk());
        assertEquals(token, customers.findByUserName("failure").getActiveLoginToken());
        mvc.perform(get("/mypage").session(browser).param("_tab", valid)).andExpect(status().isOk());
    }

    @Test
    void simultaneousLoginsHaveExactlyOneValidWinner() throws Exception {
        MockHttpSession initial = new MockHttpSession();
        login(initial, "race");
        MockHttpSession first = new MockHttpSession();
        MockHttpSession second = new MockHttpSession();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return loginExisting(first, "race");
            });
            var b = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return loginExisting(second, "race");
            });
            start.countDown();
            String firstTab = a.get(10, TimeUnit.SECONDS);
            String secondTab = b.get(10, TimeUnit.SECONDS);
            int firstStatus = mvc.perform(get("/mypage").session(first).param("_tab", firstTab))
                    .andReturn().getResponse().getStatus();
            int secondStatus = mvc.perform(get("/mypage").session(second).param("_tab", secondTab))
                    .andReturn().getResponse().getStatus();
            assertTrue((firstStatus == 200 && secondStatus == 302)
                    || (firstStatus == 302 && secondStatus == 200));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void twoAccountsInOneBrowserHaveIndependentMvcModelsApiAndLogout() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        String alice = login(browser, "tabAlice");
        String bob = login(browser, "tabBob");
        mvc.perform(get("/mypage").session(browser).param("_tab", alice))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("tabAlice")));
        mvc.perform(get("/mypage").session(browser).param("_tab", bob))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("tabBob")));
        mvc.perform(post("/api/login-status/heartbeat").session(browser).header("X-Prosta-Tab", alice))
                .andExpect(status().isOk());
        mvc.perform(get("/friends").session(browser).param("_tab", alice))
                .andExpect(status().isOk()).andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/api/friends?_tab="))));
        mvc.perform(get("/logout").session(browser).param("_tab", bob))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/mypage").session(browser).param("_tab", alice))
                .andExpect(status().isOk());
        mvc.perform(get("/mypage").session(browser).param("_tab", bob))
                .andExpect(status().is3xxRedirection());
        assertFalse(browser.isInvalid());
    }

    @Test
    void simultaneousResultPostsSaveOnceAndNeverUseTheOtherTabsAccount() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        String alice = login(browser, "gradeA");
        String bob = login(browser, "gradeB");
        Category category = new Category();
        category.setCategoryName("Concurrency");
        category = categories.saveAndFlush(category);
        Question question = new Question();
        question.setCategory(category);
        question.setDifficulty(1);
        question.setQuestionText("Concurrent test");
        question.setOptionA("A");
        question.setOptionB("B");
        question.setOptionC("C");
        question.setCorrectOption(1);
        question = questions.saveAndFlush(question);
        for (String token : new String[] {alice, bob}) {
            mvc.perform(get("/test/category/start").session(browser).param("_tab", token)
                            .param("category", category.getCategoryId().toString()).param("difficulty", "1"))
                    .andExpect(status().isOk());
            mvc.perform(post("/test/check").session(browser).param("_tab", token)
                            .param("results[0].questionId", question.getQuestionId().toString())
                            .param("results[0].category", category.getCategoryId().toString())
                            .param("results[0].ansOption", "1").param("results[0].correctOption", "1"))
                    .andExpect(status().isOk());
        }
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var task = (java.util.concurrent.Callable<Integer>) () -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out");
                return mvc.perform(post("/test/result").session(browser).param("_tab", alice))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            start.countDown();
            assertEquals(302, first.get(10, TimeUnit.SECONDS));
            assertEquals(302, second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        Customer a = customers.findByUserName("gradeA");
        Customer b = customers.findByUserName("gradeB");
        assertEquals(1, answers.countByCustomerAndCorrectFlagTrue(a));
        assertEquals(7L, a.getTotalExp());
        assertEquals(0, answers.countByCustomerAndCorrectFlagTrue(b));
        mvc.perform(post("/test/result").session(browser).param("_tab", bob))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, answers.countByCustomerAndCorrectFlagTrue(b));
        assertEquals(7L, customers.findByUserName("gradeB").getTotalExp());
    }

    @Test
    void failedGradingRollsBackAllAnswersAndCanBeRetried() throws Exception {
        MockHttpSession browser = new MockHttpSession();
        String tab = login(browser, "rollback");
        Category category = new Category();
        category.setCategoryName("Rollback");
        category = categories.saveAndFlush(category);
        java.util.List<Question> batch = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Question question = new Question();
            question.setCategory(category);
            question.setDifficulty(1);
            question.setQuestionText("Rollback " + i);
            question.setOptionA("A");
            question.setOptionB("B");
            question.setOptionC("C");
            question.setCorrectOption(1);
            batch.add(questions.saveAndFlush(question));
        }
        mvc.perform(get("/test/category/start").session(browser).param("_tab", tab)
                        .param("category", category.getCategoryId().toString()).param("difficulty", "1"))
                .andExpect(status().isOk());
        var check = post("/test/check").session(browser).param("_tab", tab);
        for (int i = 0; i < batch.size(); i++) {
            check.param("results[" + i + "].questionId", batch.get(i).getQuestionId().toString())
                    .param("results[" + i + "].ansOption", "1");
        }
        check.param("results[0].correctOption", "1");
        mvc.perform(check).andExpect(status().isOk());
        assertThrows(jakarta.servlet.ServletException.class,
                () -> mvc.perform(post("/test/result").session(browser).param("_tab", tab)));
        Customer user = customers.findByUserName("rollback");
        assertEquals(0, answers.countByCustomerAndCorrectFlagTrue(user));
        assertEquals(0L, user.getTotalExp());

        check.param("results[1].correctOption", "1");
        mvc.perform(check).andExpect(status().isOk());
        mvc.perform(post("/test/result").session(browser).param("_tab", tab))
                .andExpect(status().is3xxRedirection());
        assertEquals(2, answers.countByCustomerAndCorrectFlagTrue(user));
        assertEquals(9L, customers.findByUserName("rollback").getTotalExp());
    }
}
