package Models;


public class Group {
    public int id;
    public String groupName;
    public String createdAt;

    public Group(int id, String groupName, String createdAt) {
        this.id = id;
        this.groupName = groupName;
        this.createdAt = createdAt;
    }
    
    public Group(){
        
    }
    
    
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
}