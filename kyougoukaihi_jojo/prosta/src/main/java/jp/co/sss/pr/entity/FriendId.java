package jp.co.sss.pr.entity;

import java.io.Serializable;
import java.util.Objects;

public class FriendId implements Serializable {

    private Integer senderUserId;
    private Integer receiverUserId;

    public FriendId() {
    }

    public FriendId(Integer senderUserId, Integer receiverUserId) {
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FriendId)) {
            return false;
        }
        FriendId that = (FriendId) other;
        return Objects.equals(senderUserId, that.senderUserId)
                && Objects.equals(receiverUserId, that.receiverUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderUserId, receiverUserId);
    }
}
