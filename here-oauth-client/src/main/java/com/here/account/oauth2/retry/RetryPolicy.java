package com.here.account.oauth2.retry;

/**
 * A {@link RetryPolicy} is responsible for providing policy and condition for retries.
 */
public interface RetryPolicy {

    /**
     * Decides whether retry should happen based on last response or last exception thrown.
     * @param retryContext An instance of {@link RetryContext}
     * @return true if retry should happen otherwise false.
     */
    boolean shouldRetry(RetryContext retryContext);

    /**
     * Calculates wait interval after which retry should happen in milliseconds.
     * @param retryContext An instance of {@link RetryContext}
     * @return wait interval in milliseconds
     */
    int getNextRetryIntervalMillis(RetryContext retryContext);
}
