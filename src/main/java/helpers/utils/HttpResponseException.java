package helpers.utils;

public class HttpResponseException extends SendAPICallException {
    private final int statusCode;
    private final String responseBody;

    public HttpResponseException(int statusCode, String responseBody) {
        super("HTTP Error: " + statusCode + " - " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}