package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.UserAchievement;
import jp.co.sss.pr.repository.AchievementRepository;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;
import jakarta.persistence.EntityManager;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LevelServiceTest {

    @Mock
    private EntityManager entityManager;

    @BeforeEach
    void managedCustomer() {
        lenient().when(entityManager.contains(any())).thenReturn(true);
    }

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestService questService;

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

    @ParameterizedTest
    @CsvSource({"24, false, false", "25, true, false", "49, true, false", "50, true, true"})
    void levelAchievementsUseSqlIdsAtThresholds(int level, boolean unlock25, boolean unlock50) {
        Customer customer = customerWithLogin(LocalDate.now(), 1, 1);
        customer.setTotalExp((level - 1) * 15L);
        Achievement level25 = new Achievement();
        level25.setAchievementId(6);
        Achievement level50 = new Achievement();
        level50.setAchievementId(7);
        when(achievementRepository.findById(any())).thenAnswer(invocation -> {
            int id = invocation.getArgument(0);
            return id == 6 ? Optional.of(level25) : id == 7 ? Optional.of(level50) : Optional.empty();
        });

        List<Achievement> unlocked = levelService.processTestResult(customer, 0, 0);

        assertEquals(unlock25, unlocked.contains(level25));
        assertEquals(unlock50, unlocked.contains(level50));
        var saved = org.mockito.ArgumentCaptor.forClass(UserAchievement.class);
        verify(userAchievementRepository, org.mockito.Mockito.times(
                (unlock25 ? 1 : 0) + (unlock50 ? 1 : 0))).save(saved.capture());
        assertEquals(unlocked.stream().map(achievement -> achievement.getAchievementId()).toList(),
                saved.getAllValues().stream().map(ua -> ua.getAchievement().getAchievementId()).toList());
    }

    @Test
    void allQuestsAddTenExpOnlyOnceAndRaiseLevel() {
        Customer customer = customerWithLogin(LocalDate.now(), 1, 1);
        when(questService.allCompleted(customer, LocalDate.now())).thenReturn(true);

        levelService.processTestResult(customer, 4, 5);
        assertEquals(18L, customer.getTotalExp());
        assertEquals(2, customer.getUserLevel());
        assertEquals(LocalDate.now(), customer.getQuestRewardDate());

        levelService.processTestResult(customer, 1, 2);
        assertEquals(20L, customer.getTotalExp());
    }

    @Test
    void incompleteQuestsKeepNormalPerfectScoreReward() {
        Customer customer = customerWithLogin(LocalDate.now(), 1, 1);
        levelService.processTestResult(customer, 2, 2);
        assertEquals(9L, customer.getTotalExp());
        org.junit.jupiter.api.Assertions.assertNull(customer.getQuestRewardDate());
    }

    @Test
    void previousDaysRewardDoesNotBlockTodaysReward() {
        Customer customer = customerWithLogin(LocalDate.now(), 1, 1);
        customer.setQuestRewardDate(LocalDate.now().minusDays(1));
        when(questService.allCompleted(customer, LocalDate.now())).thenReturn(true);
        levelService.processTestResult(customer, 5, 5);
        assertEquals(25L, customer.getTotalExp());
        assertEquals(LocalDate.now(), customer.getQuestRewardDate());
    }

    @Test
    void emptyTestCannotAwardQuestReward() {
        Customer customer = customerWithLogin(LocalDate.now(), 1, 1);
        levelService.processTestResult(customer, 0, 0);
        assertEquals(0L, customer.getTotalExp());
        org.mockito.Mockito.verifyNoInteractions(questService);
    }
}
