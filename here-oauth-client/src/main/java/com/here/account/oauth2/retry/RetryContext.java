package com.here.account.oauth2.retry;

import com.here.account.http.HttpProvider;

/**
 * A {@link RetryContext} contains data set to be used for next retry.
 */
public class RetryContext {

    private int retryCount;
    private HttpProvider.HttpResponse lastRetryResponse;
    private Exception lastException;

    public int getRetryCount() {
        return this.retryCount;
    }

    /**
     * Increments the retryCount by 1.
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    public HttpProvider.HttpResponse getLastRetryResponse() {
        return lastRetryResponse;
    }

    public void setLastRetryResponse(HttpProvider.HttpResponse lastRetryResponse) {
        this.lastRetryResponse = lastRetryResponse;
    }

    public Exception getLastException() {
        return lastException;
    }

    public void setLastException(Exception lastException) {
        this.lastException = lastException;
    }
}
