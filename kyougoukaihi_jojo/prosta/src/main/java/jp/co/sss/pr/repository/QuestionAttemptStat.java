package jp.co.sss.pr.repository;

/** 問題ごとの解答回数・正解回数の集計結果。 */
public interface QuestionAttemptStat {

    Integer getQuestionId();

    Long getAttemptCount();

    Long getCorrectCount();
}
