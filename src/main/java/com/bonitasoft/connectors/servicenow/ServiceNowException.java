package com.bonitasoft.connectors.servicenow;

/**
 * Typed exception for ServiceNow connector.
 */
public class ServiceNowException extends Exception {

    private final int statusCode;
    private final boolean retryable;

    public ServiceNowException(String message) {
        super(message);
        this.statusCode = -1;
        this.retryable = false;
    }

    public ServiceNowException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.retryable = false;
    }

    public ServiceNowException(String message, int statusCode, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public ServiceNowException(String message, int statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
