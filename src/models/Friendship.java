
package models;



public class Friendship {

    private int id;
    private int userId1;
    private int userId2;
    private String status;

    public Friendship() {}

    public Friendship(int id, int userId1, int userId2, String status) {
        this.id = id;
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId1() {
        return userId1;
    }

    public void setUserId1(int userId1) {
        this.userId1 = userId1;
    }

    public int getUserId2() {
        return userId2;
    }

    public void setUserId2(int userId2) {
        this.userId2 = userId2;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

