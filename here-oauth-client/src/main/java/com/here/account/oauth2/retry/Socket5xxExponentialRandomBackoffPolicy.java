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

    private final int maxNumberOfRetries;
    private final int retryIntervalMillis;

    public Socket5xxExponentialRandomBackoffPolicy(){
        this.maxNumberOfRetries = DEFAULT_MAX_NO_RETRIES;
        this.retryIntervalMillis = DEFAULT_RETRY_INTERVAL_MILLIS;
    }

    public Socket5xxExponentialRandomBackoffPolicy(int maxNumberOfRetries, int retryIntervalMillis){
        this.maxNumberOfRetries = maxNumberOfRetries;
        this.retryIntervalMillis = retryIntervalMillis;
    }

    @Override
    public boolean shouldRetry(RetryContext retryContext) {
        return retryContext.getRetryCount() < maxNumberOfRetries
                && (retryContext.getLastException() instanceof SocketTimeoutException
                || (retryContext.getLastRetryResponse() != null
                && retryContext.getLastRetryResponse().getStatusCode() >= 500))
                || (null != retryContext.getLastException() && retryContext.getLastException().getCause() instanceof SocketTimeoutException);
    }

    @Override
    public int getNextRetryIntervalMillis(RetryContext retryContext){
        int factor = 1 << (retryContext.getRetryCount());
        return  retryIntervalMillis * ThreadLocalRandom.current().nextInt(factor);
    }
}
