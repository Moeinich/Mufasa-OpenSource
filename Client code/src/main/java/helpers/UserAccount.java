package helpers;

public class UserAccount {
    private String user;
    private String password;
    private String pin;

    public UserAccount(String user, String password, String pin) {
        this.user = user;
        this.password = password;
        this.pin = pin;
    }

    // Getters and Setters for each field
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}

