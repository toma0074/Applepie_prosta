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
import jakarta.persistence.UniqueConstraint;

/** 対戦参加者。submittedAt が null の間だけ1回提出できる。 */
@Entity
@Table(name = "friend_challenge_participant", uniqueConstraints = {
        @UniqueConstraint(name = "uk_friend_challenge_participant",
                columnNames = {"challenge_id", "user_id"})
})
public class FriendChallengeParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_friend_challenge_participant_gen")
    @SequenceGenerator(name = "seq_friend_challenge_participant_gen",
            sequenceName = "seq_friend_challenge_participant", allocationSize = 1)
    @Column(name = "participant_id")
    private Integer participantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private FriendChallenge challenge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Customer user;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public boolean isSubmitted() { return submittedAt != null; }

    public Integer getParticipantId() { return participantId; }
    public void setParticipantId(Integer participantId) { this.participantId = participantId; }
    public FriendChallenge getChallenge() { return challenge; }
    public void setChallenge(FriendChallenge challenge) { this.challenge = challenge; }
    public Customer getUser() { return user; }
    public void setUser(Customer user) { this.user = user; }
    public Integer getCorrectCount() { return correctCount; }
    public void setCorrectCount(Integer correctCount) { this.correctCount = correctCount; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}