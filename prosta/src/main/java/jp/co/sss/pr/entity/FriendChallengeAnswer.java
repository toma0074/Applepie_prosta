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

/** 対戦の各問への解答。採点はサーバー側のスナップショットで行う。 */
@Entity
@Table(name = "friend_challenge_answer", uniqueConstraints = {
        @UniqueConstraint(name = "uk_friend_challenge_answer",
                columnNames = {"participant_id", "challenge_question_id"})
})
public class FriendChallengeAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_friend_challenge_answer_gen")
    @SequenceGenerator(name = "seq_friend_challenge_answer_gen",
            sequenceName = "seq_friend_challenge_answer", allocationSize = 1)
    @Column(name = "challenge_answer_id")
    private Integer challengeAnswerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private FriendChallengeParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_question_id", nullable = false)
    private FriendChallengeQuestion challengeQuestion;

    @Column(name = "ans_option", nullable = false)
    private Integer ansOption;

    @Column(name = "correct_flag", nullable = false)
    private Boolean correctFlag;

    public Integer getChallengeAnswerId() { return challengeAnswerId; }
    public void setChallengeAnswerId(Integer challengeAnswerId) { this.challengeAnswerId = challengeAnswerId; }
    public FriendChallengeParticipant getParticipant() { return participant; }
    public void setParticipant(FriendChallengeParticipant participant) { this.participant = participant; }
    public FriendChallengeQuestion getChallengeQuestion() { return challengeQuestion; }
    public void setChallengeQuestion(FriendChallengeQuestion challengeQuestion) { this.challengeQuestion = challengeQuestion; }
    public Integer getAnsOption() { return ansOption; }
    public void setAnsOption(Integer ansOption) { this.ansOption = ansOption; }
    public Boolean getCorrectFlag() { return correctFlag; }
    public void setCorrectFlag(Boolean correctFlag) { this.correctFlag = correctFlag; }
}