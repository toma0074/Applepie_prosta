package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import jp.co.sss.pr.repository.DailyStudySummary;

class LearningTrendServiceTest {
    private final LocalDate today = LocalDate.of(2026, 11, 4);

    @Test
    void fillsEightMondayWeeksAndUsesWeightedAnswerAccuracy() {
        var trend = LearningTrendService.summarize(today, List.of(
                day(LocalDate.of(2026, 10, 26), 1, 1),
                day(LocalDate.of(2026, 11, 1), 9, 0),
                day(LocalDate.of(2026, 11, 2), 5, 3),
                day(today, 5, 5),
                day(today.plusDays(1), 100, 100),
                day(LocalDate.of(2026, 9, 13), 100, 100)));
        assertEquals(8, trend.weeks().size());
        assertEquals(LocalDate.of(2026, 9, 14), trend.weeks().get(0).start());
        assertEquals(LocalDate.of(2026, 11, 1), trend.weeks().get(6).end());
        assertEquals(today, trend.weeks().get(7).end());
        assertEquals(10, trend.weeks().get(6).accuracy());
        assertEquals(80, trend.weeks().get(7).accuracy());
        assertEquals(0, trend.answerDelta());
        assertEquals(70, trend.accuracyDelta());
        assertEquals(100, trend.weeks().get(7).answerBarPercent());
        assertNull(trend.weeks().get(0).accuracy());
    }

    @Test
    void emptyWeeksHaveNoAccuracyAndNoAccuracyDelta() {
        var empty = LearningTrendService.summarize(today, List.of());
        assertEquals(0, empty.answerDelta());
        assertNull(empty.accuracyDelta());
        empty.weeks().forEach(week -> {
            assertEquals(0, week.answerCount());
            assertEquals(0, week.answerBarPercent());
            assertNull(week.accuracy());
        });
        var stopped = LearningTrendService.summarize(today,
                List.of(day(LocalDate.of(2026, 10, 26), 4, 0)));
        assertEquals(-4, stopped.answerDelta());
        assertNull(stopped.accuracyDelta());
        assertEquals(0, stopped.weeks().get(6).accuracy());
        var started = LearningTrendService.summarize(today, List.of(day(today, 3, 1)));
        assertEquals(3, started.answerDelta());
        assertNull(started.accuracyDelta());
    }

    @Test
    void accuracyDeltaCanDecreaseAndWeekStartsOnMondayEvenOnSunday() {
        var trend = LearningTrendService.summarize(LocalDate.of(2026, 11, 8), List.of(
                day(LocalDate.of(2026, 11, 1), 2, 2),
                day(LocalDate.of(2026, 11, 8), 4, 1)));
        assertEquals(LocalDate.of(2026, 11, 2), trend.weeks().get(7).start());
        assertEquals(2, trend.answerDelta());
        assertEquals(-75, trend.accuracyDelta());
    }

    private DailyStudySummary day(LocalDate date, long count, long correct) {
        DailyStudySummary day = mock(DailyStudySummary.class);
        when(day.getStudyDate()).thenReturn(date);
        org.mockito.Mockito.lenient().when(day.getAnswerCount()).thenReturn(count);
        org.mockito.Mockito.lenient().when(day.getCorrectCount()).thenReturn(correct);
        return day;
    }
}
