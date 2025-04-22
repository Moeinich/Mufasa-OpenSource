package osr.mapping.utils;

public enum LoginMessages {
    NONE(""),
    CONNECTING("Connecting to server"),
    CONNECTING_UPDATESERVER("Connecting to update server"),
    INVALID_CREDENTIALS("Invalid credentials"),
    NEED_SKILL_TOTAL("You need a skill total of"),
    INVALID_USER_PASS("Please enter your username/email address."),
    ERROR_CONNECTING("Error connecting to server"),
    ACCOUNT_NOT_LOGGED_OUT("Your account has not logged out"),
    LOGIN_SERVER_OFFLINE("Login server offline"),
    ERROR_LOADING_PROFILE("Error loading your profile"),
    CONNECTION_TIMED_OUT("Connection timed out"),
    LOGIN_LIMIT_EXCEEDED("Login limit exceeded"),
    WORLD_FULL("This world is full"),
    ACCOUNT_DISABLED("Your account has been disabled"),
    ACCOUNT_RULE_BREAKER("Your account has been involved"),
    MEMBERS("You need a members account"),
    MEMBERS2("Subscribe or use a different"),
    IN_MEMBERS_AREA("You are standing in a members-only area"),
    AUTHENTICATOR("Authenticator"),
    DATE_OF_BIRTH("Your date of birth isn't set."),
    SIGN_OUT_CONFIRMATION("Are you sure you want to sign out?"),
    SIGN_IN_TO_GOOGLE("Sign in with Google"),
    SIGN_IN_EMAIL("Sign in with Email / Username"),
    DISCONNECTED("You were disconnected from the server."),
    ACCOUNT_NOT_LOGGED_OUT2("Either your account is still logged in"),
    USE_DIFFERENT_WORLD("Please use a different world"),
    UPDATE_MSG_1("The game servers are currently being updated."),
    UPDATE_MSG_2("Please wait a few minutes and try again."),
    UPDATE_MSG_3("servers are currently being"),
    UPDATE_MSG_4("wait a few minutes and"),
    BETA_MSG_1("This is a Beta world"),
    BETA_MSG_2("Your normal account will not be affected");

    private final String message;

    LoginMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

