package jp.co.sss.pr.entity;

import java.time.LocalDateTime;

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

/** フレンド対戦（非同期）。出題内容は作成時に FriendChallengeQuestion へ固定する。 */
@Entity
@Table(name = "friend_challenge")
public class FriendChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_friend_challenge_gen")
    @SequenceGenerator(name = "seq_friend_challenge_gen",
            sequenceName = "seq_friend_challenge", allocationSize = 1)
    @Column(name = "challenge_id")
    private Integer challengeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Customer creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opponent_id", nullable = false)
    private Customer opponent;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "difficulty", nullable = false)
    private Integer difficulty;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Integer getChallengeId() { return challengeId; }
    public void setChallengeId(Integer challengeId) { this.challengeId = challengeId; }
    public Customer getCreator() { return creator; }
    public void setCreator(Customer creator) { this.creator = creator; }
    public Customer getOpponent() { return opponent; }
    public void setOpponent(Customer opponent) { this.opponent = opponent; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}