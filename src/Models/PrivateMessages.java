package Models;


public class PrivateMessages {
    public int id;
    public int friendshipId;
    public int senderId;
    public String message;
    public String sentAt;

    public PrivateMessages(){
        
    }
    public PrivateMessages(int id, int friendshipId, int senderId, String message, String sentAt) {
        this.id = id;
        this.friendshipId = friendshipId;
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

    public int getFriendshipId() {
        return friendshipId;
    }

    public void setFriendshipId(int friendshipId) {
        this.friendshipId = friendshipId;
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

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }
    
    
}