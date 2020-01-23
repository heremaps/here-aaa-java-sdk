package com.here.account.oauth2.retry;

/**
 * An implementation of {@code RetryPolicy} with default behaviour as no retry.
 */
public class NoRetryPolicy implements RetryPolicy {

    @Override
    public boolean shouldRetry(RetryContext retryContext) {
        return false;
    }

    @Override
    public int getNextRetryIntervalMillis(RetryContext retryContext) {
        return 0;
    }
}
