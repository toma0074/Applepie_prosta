package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.AchievementRepository;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;

@ExtendWith(MockitoExtension.class)
class LevelServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @InjectMocks
    private LevelService levelService;

    @Test
    void incrementsStreakAfterLoginOnPreviousDay() {
        Customer customer = customerWithLogin(LocalDate.now().minusDays(1), 0, 4);
        when(answerRepository.countByCustomerAndCorrectFlagTrue(customer)).thenReturn(0);
        when(customerRepository.save(customer)).thenReturn(customer);

        levelService.processLogin(customer);

        assertEquals(1, customer.getLoginCount());
        assertEquals(5, customer.getConsecutiveLoginDays());
        assertEquals(LocalDate.now(), customer.getLoginDate());
        verify(customerRepository).save(customer);
    }

    @Test
    void resetsStreakAfterMissedDay() {
        Customer customer = customerWithLogin(LocalDate.now().minusDays(2), 3, 9);
        when(answerRepository.countByCustomerAndCorrectFlagTrue(customer)).thenReturn(0);
        when(customerRepository.save(customer)).thenReturn(customer);

        levelService.processLogin(customer);

        assertEquals(4, customer.getLoginCount());
        assertEquals(1, customer.getConsecutiveLoginDays());
    }

    @Test
    void loginDoesNotRaiseLevelWithoutExp() {
        Customer customer = customerWithLogin(LocalDate.now().minusDays(1), 6, 0);
        when(answerRepository.countByCustomerAndCorrectFlagTrue(customer)).thenReturn(0);
        when(customerRepository.save(customer)).thenReturn(customer);

        levelService.processLogin(customer);

        assertEquals(7, customer.getLoginCount());
        assertEquals(1, customer.getConsecutiveLoginDays());
        assertEquals(1, customer.getUserLevel());
    }

    @Test
    void doesNotIncrementLoginOrStreakTwiceOnSameDay() {
        Customer customer = customerWithLogin(LocalDate.now(), 5, 3);
        customer.setLoginFlag(false);
        when(customerRepository.save(customer)).thenReturn(customer);

        assertTrue(levelService.processLogin(customer).isEmpty());

        assertEquals(5, customer.getLoginCount());
        assertEquals(3, customer.getConsecutiveLoginDays());
        verify(customerRepository).save(customer);
    }

    private Customer customerWithLogin(LocalDate lastLoginDate, int loginCount, int streak) {
        Customer customer = new Customer();
        customer.setUserId(1);
        customer.setDeleteFlag(false);
        customer.setPermission(0);
        customer.setLoginDate(lastLoginDate);
        customer.setLoginCount(loginCount);
        customer.setConsecutiveLoginDays(streak);
        customer.setUserLevel(1);
        return customer;
    }
}
