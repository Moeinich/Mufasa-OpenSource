package helpers.scripts;

public class CancellationToken {
    private volatile boolean cancellationRequested = false;
    private static final CancellationToken defaultToken = new CancellationToken(); // Static default token

    public void requestCancellation() {
        cancellationRequested = true;
    }

    public boolean isCancellationRequested() {
        return cancellationRequested;
    }

    // Static method to get a default cancelled token
    public static CancellationToken getDefaultCancelledToken() {
        defaultToken.requestCancellation(); // Set this token as cancelled by default
        return defaultToken;
    }
}
