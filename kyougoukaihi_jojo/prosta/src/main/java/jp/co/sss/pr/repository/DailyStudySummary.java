package jp.co.sss.pr.repository;

import java.time.LocalDate;

public interface DailyStudySummary {

    LocalDate getStudyDate();

    Long getAnswerCount();

    Long getCorrectCount();
}
