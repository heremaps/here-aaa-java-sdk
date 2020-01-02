package com.here.account.oauth2.retry;

import com.here.account.http.HttpProvider;

/**
 * A {@link RetryContext} contains data set to be used for next retry.
 */
public class RetryContext {

    private int retryCount;
    private HttpProvider.HttpResponse lastRetryResponse;
    private Throwable lastException;

    public int getRetryCount() {
        return this.retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public HttpProvider.HttpResponse getLastRetryResponse() {
        return lastRetryResponse;
    }

    public void setLastRetryResponse(HttpProvider.HttpResponse lastRetryResponse) {
        this.setLastThrowable(null);
        this.lastRetryResponse = lastRetryResponse;
    }

    public Throwable getLastThrowable() {
        return lastException;
    }

    public void setLastThrowable(Throwable lastException) {
        this.lastException = lastException;
    }
}
