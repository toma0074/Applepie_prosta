package jp.co.sss.pr.form;

/** 学習メモの入力値。文字数などの検証は QuestionStudyService で行う。 */
public class QuestionNoteForm {

    private String content;
    private String returnUrl;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
}