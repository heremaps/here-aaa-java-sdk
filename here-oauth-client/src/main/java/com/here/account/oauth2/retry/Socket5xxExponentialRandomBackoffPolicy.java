package com.here.account.oauth2.retry;

import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An implementation of {@link RetryPolicy} to retry on {@code SocketTimeoutException}
 * and http status greater or equal to 500
 */
public class Socket5xxExponentialRandomBackoffPolicy implements RetryPolicy {

    public static final int DEFAULT_MAX_NO_RETRIES = 3;
    public static final int DEFAULT_RETRY_INTERVAL_MILLIS = 1000;
    public static final int DEFAULT_MAX_RETRY_FACTOR = 1 << 30;

    private final int maxNumberOfRetries;
    private final int retryIntervalMillis;
    private final int maxRetryFactor;

    public Socket5xxExponentialRandomBackoffPolicy(){
        this(DEFAULT_MAX_NO_RETRIES, DEFAULT_RETRY_INTERVAL_MILLIS);
    }

    public Socket5xxExponentialRandomBackoffPolicy(int maxNumberOfRetries, int retryIntervalMillis){
        this(maxNumberOfRetries, retryIntervalMillis, DEFAULT_MAX_RETRY_FACTOR);
    }

    public Socket5xxExponentialRandomBackoffPolicy(int maxNumberOfRetries, int retryIntervalMillis,
                                                   int maxRetryFactor) {
        this.maxNumberOfRetries = maxNumberOfRetries;
        this.retryIntervalMillis = retryIntervalMillis;
        this.maxRetryFactor = maxRetryFactor;
    }

    @Override
    public boolean shouldRetry(RetryContext retryContext) {
        return retryContext.getRetryCount() < maxNumberOfRetries
                && (retryContext.getLastException() instanceof SocketTimeoutException
                || (retryContext.getLastRetryResponse() != null
                && retryContext.getLastRetryResponse().getStatusCode() >= 500))
                || (null != retryContext.getLastException() && retryContext.getLastException().getCause() instanceof SocketTimeoutException);
    }

    /**
     * Employs the exponential random backoff policy using a base of 2 and exponent of number of retries,
     * up to a maximum, subject to the configured maxRetryFactor, and multiplied by the retryIntervalMillis.
     *
     * @param retryContext An instance of {@link RetryContext}
     * @return the next retry interval in milliseconds
     */
    @Override
    public int getNextRetryIntervalMillis(RetryContext retryContext){
        int retryCount = retryContext.getRetryCount();
        int factor = Math.min(1 << (Math.min(retryCount, 30)), maxRetryFactor);
        return  retryIntervalMillis * ThreadLocalRandom.current().nextInt(factor);
    }
}
