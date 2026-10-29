package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.mypage_function.FriendFeedController;
import jp.co.sss.pr.repository.AnswerRepository;

class FriendFeedRollbackTest extends ValidationSupport {
    @Autowired FriendFeedController feed;
    @Autowired PlatformTransactionManager transactions;
    @SpyBean AnswerRepository failingAnswers;

    @Test
    void databaseFailureStillRollsBackTheEnclosingTransaction() {
        acceptedFriendship();
        CustomerBean bean = new CustomerBean();
        bean.setUserId(user.getUserId());
        browser.setAttribute("user", bean);
        String originalName = user.getUserName();
        doThrow(new DataAccessResourceFailureException("Simulated database outage"))
                .when(failingAnswers).summarizeDailyStudy(any(Customer.class), any(LocalDate.class), any(LocalDate.class));

        assertThrows(DataAccessResourceFailureException.class,
                () -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    Customer current = customers.findById(user.getUserId()).orElseThrow();
                    current.setUserName("changed");
                    customers.saveAndFlush(current);
                    feed.feed(friend.getUserId(), browser);
                }));

        assertEquals(originalName, customers.findById(user.getUserId()).orElseThrow().getUserName());
    }
}
