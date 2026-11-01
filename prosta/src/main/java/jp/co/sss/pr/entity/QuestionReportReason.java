package jp.co.sss.pr.entity;

/** 問題・解説の報告理由 */
public enum QuestionReportReason {
    WRONG_ANSWER("正解が誤っている"),
    UNCLEAR_EXPLANATION("解説がわかりにくい"),
    OTHER("その他");

    private final String label;

    QuestionReportReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}