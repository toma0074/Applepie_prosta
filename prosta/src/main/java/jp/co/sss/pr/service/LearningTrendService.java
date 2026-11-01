package jp.co.sss.pr.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import jp.co.sss.pr.repository.DailyStudySummary;

/** Weekly presentation of the shared daily answer aggregation. */
public final class LearningTrendService {
    private LearningTrendService() {
    }

    public static LocalDate firstWeek(LocalDate today) {
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(7);
    }

    public static Trend summarize(LocalDate today, List<DailyStudySummary> days) {
        LocalDate first = firstWeek(today);
        long[] answers = new long[8];
        long[] correct = new long[8];
        for (DailyStudySummary day : days) {
            LocalDate date = day.getStudyDate();
            if (date == null || date.isBefore(first) || date.isAfter(today)) {
                continue;
            }
            int index = (int) ((date.toEpochDay() - first.toEpochDay()) / 7);
            answers[index] += day.getAnswerCount();
            correct[index] += day.getCorrectCount();
        }
        long maximum = 1;
        for (long count : answers) {
            maximum = Math.max(maximum, count);
        }
        List<Week> weeks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            LocalDate start = first.plusWeeks(i);
            weeks.add(new Week(start, i == 7 ? today : start.plusDays(6),
                    answers[i], correct[i], accuracy(answers[i], correct[i]),
                    (int) Math.round(answers[i] * 100.0 / maximum)));
        }
        Week current = weeks.get(7);
        Week previous = weeks.get(6);
        // Without observations in either week an accuracy difference is undefined, not 0%.
        Integer accuracyDelta = current.accuracy() == null || previous.accuracy() == null
                ? null : current.accuracy() - previous.accuracy();
        return new Trend(List.copyOf(weeks), current.answerCount() - previous.answerCount(), accuracyDelta);
    }

    private static Integer accuracy(long answers, long correct) {
        return answers == 0 ? null : (int) Math.round(correct * 100.0 / answers);
    }

    public record Week(LocalDate start, LocalDate end, long answerCount, long correctCount,
            Integer accuracy, int answerBarPercent) {
    }

    public record Trend(List<Week> weeks, long answerDelta, Integer accuracyDelta) {
    }
}
