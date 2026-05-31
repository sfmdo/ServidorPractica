
package models;


public class GroupMessages {
   public int id;
   public int groupId;
   public int senderId;
   public String message;
   public String sentAt;

    public GroupMessages() {
    }
    public GroupMessages(int id, int groupId, int senderId, String message, String sentAt) {
        this.id = id;
        this.groupId = groupId;
        this.senderId = senderId;
        this.message = message;
        this.sentAt = sentAt;
    }
   
   
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenAt() {
        return sentAt;
    }

    public void setSenAt(String sentAt) {
        this.sentAt = sentAt;
    }
   
}
