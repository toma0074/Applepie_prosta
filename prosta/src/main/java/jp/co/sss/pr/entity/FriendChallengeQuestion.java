package jp.co.sss.pr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 対戦作成時点の問題スナップショット。元の問題が編集・削除されても両者は同じ内容で解答する。 */
@Entity
@Table(name = "friend_challenge_question", uniqueConstraints = {
        @UniqueConstraint(name = "uk_friend_challenge_question_pos",
                columnNames = {"challenge_id", "question_order"})
})
public class FriendChallengeQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_friend_challenge_question_gen")
    @SequenceGenerator(name = "seq_friend_challenge_question_gen",
            sequenceName = "seq_friend_challenge_question", allocationSize = 1)
    @Column(name = "challenge_question_id")
    private Integer challengeQuestionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private FriendChallenge challenge;

    @Column(name = "question_order", nullable = false)
    private Integer position;

    /** 元の問題ID（解答履歴用。外部キーは張らない） */
    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    @Column(name = "option_a", nullable = false, length = 300)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 300)
    private String optionB;

    @Column(name = "option_c", nullable = false, length = 300)
    private String optionC;

    @Column(name = "correct_option", nullable = false)
    private Integer correctOption;

    @Column(name = "explanation", length = 2000)
    private String explanation;

    public Integer getChallengeQuestionId() { return challengeQuestionId; }
    public void setChallengeQuestionId(Integer challengeQuestionId) { this.challengeQuestionId = challengeQuestionId; }
    public FriendChallenge getChallenge() { return challenge; }
    public void setChallenge(FriendChallenge challenge) { this.challenge = challenge; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public Integer getCorrectOption() { return correctOption; }
    public void setCorrectOption(Integer correctOption) { this.correctOption = correctOption; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}