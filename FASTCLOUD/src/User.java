import javafx.scene.canvas.*;

public class User {
    private String uid;
    private String username;
    private String role;
    private String status;

    public User() {}

    public User(String uid, String username, String role, String status) {
        this.uid      = uid;
        this.username = username;
        this.role     = role;
        this.status   = status;
    }

    public String getUid()      { return uid; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }
    public String getStatus()   { return status; }

    public void setUid(String uid)           { this.uid = uid; }
    public void setUsername(String username) { this.username = username; }
    public void setRole(String role)         { this.role = role; }
    public void setStatus(String status)     { this.status = status; }
}