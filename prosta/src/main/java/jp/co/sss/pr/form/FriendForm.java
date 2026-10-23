package jp.co.sss.pr.form;

public class FriendForm {

    private Integer senderUserId;
    private Integer receiverUserId;
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
