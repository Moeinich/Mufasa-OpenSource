package helpers.utils;

public class SendAPICallException extends Exception {
    public SendAPICallException(String message) {
        super(message);
    }

    public SendAPICallException(String message, Throwable cause) {
        super(message, cause);
    }
}