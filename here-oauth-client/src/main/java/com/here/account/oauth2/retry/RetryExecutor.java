package com.here.account.oauth2.retry;

import com.here.account.http.HttpProvider;

import java.util.logging.Logger;

/**
 * A {@code RetryExecutor} provides a mechanisms to execute a {@link Retryable}
 * and will retry on failure according to an implementation specific retry policy.
 * */
public class RetryExecutor {

    private final RetryPolicy retryPolicy;
    private final RetryContext retryContext;
    private static final Logger LOGGER = Logger.getLogger(RetryExecutor.class.getName());

    public RetryExecutor(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        this.retryContext = new RetryContext();
    }

    /**
     * Execute the given {@link Retryable} until retry policy decides to retry.
     * @param retryable the {@link Retryable} to execute
     * @return http response return from {@code Retryable}
     * @throws Throwable
     */
    public HttpProvider.HttpResponse execute(Retryable retryable) throws Throwable {
        HttpProvider.HttpResponse httpResponse;
        try {
            httpResponse = retryable.execute();
            retryContext.setLastRetryResponse(httpResponse);
        } catch (Throwable e) {
            retryContext.setLastThrowable(e);
        }

        while (retryPolicy.shouldRetry(retryContext)) {
            try {
                retryContext.incrementRetryCount();

                int waitInterval = retryPolicy.getNextRetryIntervalMillis(retryContext);

                LOGGER.warning("Retrying after - "+ waitInterval +" milliseconds...");
                Thread.sleep(waitInterval);

                httpResponse = retryable.execute();
                retryContext.setLastRetryResponse(httpResponse);

            } catch (Throwable e) {
                retryContext.setLastThrowable(e);
            }
        }

        if (retryContext.getLastThrowable() != null) {
            throw retryContext.getLastThrowable();
        }

        return retryContext.getLastRetryResponse();
    }
}
