package jp.co.sss.pr.service;

/** 学習メモ・報告の操作で利用者に表示する日本語メッセージを持つ例外 */
public class QuestionStudyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QuestionStudyException(String message) {
        super(message);
    }
}