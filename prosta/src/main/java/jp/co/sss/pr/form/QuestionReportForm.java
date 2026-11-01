package jp.co.sss.pr.form;

/** 問題・解説の報告の入力値。理由・文字数の検証は QuestionStudyService で行う。 */
public class QuestionReportForm {

    private String reason;
    private String detail;
    private String returnUrl;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
}