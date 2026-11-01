package jp.co.sss.pr.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Favorite;
import jp.co.sss.pr.entity.Question;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class FavoriteRepositoryTest {
    @Autowired
    private FavoriteRepository favorites;
    @Autowired
    private TestEntityManager entities;

    private Customer customer(String name) {
        Customer c = new Customer();
        c.setUserName(name);
        c.setUserPass("test");
        c.setPermission(0);
        c.setDeleteFlag(false);
        return entities.persist(c);
    }

    private Question question(int difficulty) {
        Question q = new Question();
        q.setDifficulty(difficulty);
        q.setQuestionText("Question");
        q.setOptionA("A");
        q.setOptionB("B");
        q.setOptionC("C");
        q.setCorrectOption(1);
        return entities.persist(q);
    }

    private void bookmark(Customer customer, Question question) {
        Favorite f = new Favorite();
        f.setCustomer(customer);
        f.setQuestion(question);
        favorites.save(f);
    }

    @Test
    void savesReadsAndRemovesOnlyTheCurrentUsersBookmarks() {
        Customer me = customer("me");
        Customer other = customer("other");
        Question easy = question(1);
        Question hard = question(3);
        Question othersOnly = question(2);
        bookmark(me, hard);
        bookmark(me, easy);
        bookmark(other, easy);
        bookmark(other, othersOnly);
        entities.flush();
        entities.clear();

        assertEquals(List.of(easy.getQuestionId(), hard.getQuestionId()),
                favorites.findBookmarkedQuestions(me.getUserId()).stream()
                        .map(q -> q.getQuestionId()).toList());
        favorites.deleteByCustomerAndQuestion(me, easy);
        entities.flush();
        entities.clear();

        assertEquals(List.of(hard.getQuestionId()),
                favorites.findBookmarkedQuestions(me.getUserId()).stream()
                        .map(q -> q.getQuestionId()).toList());
        assertEquals(List.of(easy.getQuestionId(), othersOnly.getQuestionId()),
                favorites.findBookmarkedQuestions(other.getUserId()).stream()
                        .map(q -> q.getQuestionId()).toList());
    }

    @Test
    void noBookmarksReturnsAnEmptyList() {
        Customer me = customer("empty");
        assertEquals(List.of(), favorites.findBookmarkedQuestions(me.getUserId()));
    }

    @Test
    void legacyDuplicateBookmarksDoNotDuplicateQuestions() {
        Customer me = customer("duplicates");
        Question q = question(1);
        bookmark(me, q);
        bookmark(me, q);
        entities.flush();
        entities.clear();
        assertEquals(List.of(q.getQuestionId()),
                favorites.findBookmarkedQuestions(me.getUserId()).stream()
                        .map(question -> question.getQuestionId()).toList());
    }
}
