package jp.co.sss.pr.entity;

/** 問題・解説の報告の対応状況 */
public enum QuestionReportStatus {
    OPEN("未対応"),
    RESOLVED("対応済み"),
    ON_HOLD("保留");

    private final String label;

    QuestionReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}