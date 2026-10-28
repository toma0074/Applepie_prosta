package jp.co.sss.pr.repository;

import java.time.LocalDate;

public interface RecentStudyActivity {

    LocalDate getStudyDate();

    String getCategoryName();

    String getQuestionText();

    Boolean getCorrectFlag();

    String getOptionA();

    String getOptionB();

    String getOptionC();

    String getExplanation();

    Integer getAnsOption();

    Integer getCorrectOption();
}
