package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import jp.co.sss.pr.entity.*;
import jp.co.sss.pr.repository.AnswerRepository;

class StudyReviewServiceTest {
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final StudyReviewService service = new StudyReviewService(answers);
    private final Customer user = new Customer();
    private final LocalDate today = LocalDate.of(2026, 11, 2);
    private Answer answer(int id, LocalDate date, boolean correct) {
        Question q = new Question(); q.setQuestionId(id);
        Answer a = new Answer(); a.setQuestion(q); a.setAnsDate(date);
        a.setCorrectFlag(correct); a.setAnsOption(correct ? 1 : 2); return a;
    }
    @Test
    void wrongAnswersAreDueTomorrowAndCorrectStreaksExtendIntervals() {
        when(answers.findAllByCustomerOrderByAnsIdDesc(user)).thenReturn(List.of(
                answer(1, today.minusDays(1), false),
                answer(2, today, true), answer(2, today.minusDays(3), true),
                answer(3, today.minusDays(8), true), answer(3, today.minusDays(11), true),
                answer(3, today.minusDays(14), true)));
        var schedule = service.schedule(user);
        assertEquals(2, service.due(user, today).size());
        assertEquals(today.minusDays(1), schedule.get(0).nextDate());
        assertEquals(today.plusDays(3), schedule.get(2).nextDate());
    }
    @Test
    void sameDayRepetitionsDoNotExtendInterval() {
        when(answers.findAllByCustomerOrderByAnsIdDesc(user)).thenReturn(List.of(
                answer(1, today, true), answer(1, today, true), answer(1, today, true)));
        assertEquals(1, service.schedule(user).get(0).correctStreak());
        assertEquals(today.plusDays(1), service.schedule(user).get(0).nextDate());
        assertTrue(service.due(user, today).isEmpty());
    }
    @Test
    void wrongAnswerResetsStreakAndUsersWithNoHistoryHaveNoReviews() {
        when(answers.findAllByCustomerOrderByAnsIdDesc(user)).thenReturn(List.of(
                answer(1, today, false), answer(1, today.minusDays(2), true)));
        assertEquals(0, service.schedule(user).get(0).correctStreak());
        assertEquals(today.plusDays(1), service.schedule(user).get(0).nextDate());
        when(answers.findAllByCustomerOrderByAnsIdDesc(user)).thenReturn(List.of());
        assertTrue(service.schedule(user).isEmpty());
    }
}
