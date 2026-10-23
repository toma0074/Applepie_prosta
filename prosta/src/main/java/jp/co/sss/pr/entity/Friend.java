package jp.co.sss.pr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "friend")
@IdClass(FriendId.class)
public class Friend {

    @Id
    @Column(name = "s_user_id")
    private Integer senderUserId;

    @Id
    @Column(name = "g_user_id")
    private Integer receiverUserId;

    @Column(name = "accept_flag", nullable = false)
    private Boolean acceptFlag;

    public Integer getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(Integer senderUserId) {
        this.senderUserId = senderUserId;
    }

    public Integer getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(Integer receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    public Boolean getAcceptFlag() {
        return acceptFlag;
    }

    public void setAcceptFlag(Boolean acceptFlag) {
        this.acceptFlag = acceptFlag;
    }
}
