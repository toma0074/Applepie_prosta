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

@Entity
@Table(name = "friend_connection", uniqueConstraints = {
        @UniqueConstraint(name = "uk_friend_connection_pair",
                columnNames = {"user_low_id", "user_high_id"})
})
public class FriendConnection {

    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_friend_connection_gen")
    @SequenceGenerator(name = "seq_friend_connection_gen",
            sequenceName = "seq_friend_connection", allocationSize = 1)
    @Column(name = "friend_connection_id")
    private Integer friendConnectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_low_id", nullable = false)
    private Customer userLow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_high_id", nullable = false)
    private Customer userHigh;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private Customer requestedBy;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Integer getFriendConnectionId() { return friendConnectionId; }
    public void setFriendConnectionId(Integer friendConnectionId) { this.friendConnectionId = friendConnectionId; }
    public Customer getUserLow() { return userLow; }
    public void setUserLow(Customer userLow) { this.userLow = userLow; }
    public Customer getUserHigh() { return userHigh; }
    public void setUserHigh(Customer userHigh) { this.userHigh = userHigh; }
    public Customer getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Customer requestedBy) { this.requestedBy = requestedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
