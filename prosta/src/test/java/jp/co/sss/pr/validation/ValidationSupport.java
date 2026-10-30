package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import jp.co.sss.pr.entity.*;
import jp.co.sss.pr.repository.*;
import jp.co.sss.pr.service.PasswordService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false", "server.address=127.0.0.1"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
abstract class ValidationSupport {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @Autowired CategoryRepository categories;
    @Autowired QuestionRepository questions;
    @Autowired AnswerRepository answers;
    @Autowired StudyPlanRepository plans;
    @Autowired FriendConnectionRepository connections;
    @Autowired AchievementRepository achievements;
    @Autowired UserAchievementRepository unlocked;
    @Autowired PasswordService passwords;
    Customer user;
    Customer friend;
    Customer admin;
    Category category;
    Question question;
    MockHttpSession browser;
    String tab;

    @BeforeEach
    void seed() {
        String suffix = UUID.randomUUID().toString().substring(0, 5);
        user = customer("U" + suffix, 0, 25);
        friend = customer("F" + suffix, 0, 75);
        admin = customer("A" + suffix, 1, 100);
        category = new Category();
        category.setCategoryName("検証Java" + suffix);
        category = categories.saveAndFlush(category);
        for (int i = 1; i <= 8; i++) {
            Question q = new Question();
            q.setCategory(category);
            q.setDifficulty(1);
            q.setQuestionText("検証問題" + i + "：整数型を選んでください。");
            q.setOptionA("int");
            q.setOptionB("String");
            q.setOptionC("double");
            q.setCorrectOption(1);
            q.setExplanation("int は整数を表す型です。");
            question = questions.saveAndFlush(q);
        }
        browser = new MockHttpSession();
    }

    Customer customer(String name, int permission, int level) {
        Customer c = new Customer();
        c.setUserName(name);
        c.setUserPass(passwords.encode("pass1234"));
        c.setPermission(permission);
        c.setDeleteFlag(false);
        c.setUserLevel(level);
        c.setTotalExp((level - 1L) * 15);
        c.setLoginDate(LocalDate.now());
        c.setLoginFlag(false);
        return customers.saveAndFlush(c);
    }

    String login(Customer c) throws Exception {
        String redirect = mvc.perform(post("/login").session(browser)
                        .param("_tab", UUID.randomUUID().toString())
                        .param("userName", c.getUserName()).param("userPass", "pass1234"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertNotNull(redirect);
        String token = UriComponentsBuilder.fromUriString(redirect).build().getQueryParams().getFirst("_tab");
        assertNotNull(token);
        return token;
    }

    void protectedPage(String path) throws Exception {
        mvc.perform(get(path).session(browser).param("_tab", tab))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("<html")));
    }

    void acceptedFriendship() {
        FriendConnection c = new FriendConnection();
        c.setUserLow(user);
        c.setUserHigh(friend);
        c.setRequestedBy(user);
        c.setStatus(FriendConnection.ACCEPTED);
        c.setCreatedAt(java.time.LocalDateTime.now());
        c.setUpdatedAt(java.time.LocalDateTime.now());
        connections.saveAndFlush(c);
    }
}
