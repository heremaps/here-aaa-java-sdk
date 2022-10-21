package com.here.account.oauth2.retry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import java.net.SocketTimeoutException;
import com.here.account.http.HttpProvider;

import static org.junit.Assert.assertTrue;

public class Socket5xxExponentialRandomBackoffPolicyTest {

    private Socket5xxExponentialRandomBackoffPolicy socket5xxExponentialRandomBackoffPolicy;
    int maxNumberOfRetries = Integer.MAX_VALUE;
    int retryIntervalMillis = 500;
    int maxRetryFactor = 5;

    @Before
    public void setUp() {
        this.socket5xxExponentialRandomBackoffPolicy =
                new Socket5xxExponentialRandomBackoffPolicy( maxNumberOfRetries,  retryIntervalMillis,
                        maxRetryFactor);
    }

    @Test
    public void test_largeRetryCount_notNegative() {
        for (int j = 0; j < 10; j++) {
            RetryContext retryContext = new RetryContext();
            for (int i = 0; i < 40; i++) {
                retryContext.incrementRetryCount();
                int nextRetryIntervalMillis = socket5xxExponentialRandomBackoffPolicy.getNextRetryIntervalMillis(retryContext);
                assertTrue("i=" + i + ", retryContext.getRetryCount()=" + retryContext.getRetryCount() + ", nextRetryIntervalMillis was negative " + nextRetryIntervalMillis, nextRetryIntervalMillis >= 0);
            }
        }
    }

    @Test
    public void test_shouldRetry() {
        HttpProvider.HttpResponse httpResponse = Mockito.mock(HttpProvider.HttpResponse.class);
        Mockito.when(httpResponse.getStatusCode()).thenReturn(503);
        Exception lastException = Mockito.mock(SocketTimeoutException.class);
        RetryContext retryContext = new RetryContext();
        retryContext.setLastRetryResponse(httpResponse);
        retryContext.setLastException(lastException);
        final int maxRetryIntervalMillis = maxRetryFactor * retryIntervalMillis;
        for (int i = 0; i < 1000; i++) {
            retryContext.incrementRetryCount();
            assertTrue("shouldRetry should have been true",
                    socket5xxExponentialRandomBackoffPolicy.shouldRetry(retryContext));
            int nextRetryIntervalMillis = socket5xxExponentialRandomBackoffPolicy.getNextRetryIntervalMillis(retryContext);
            assertTrue("unexpected nextRetryIntervalMillis "
                            + nextRetryIntervalMillis + ", should be >= 0 and <= " + maxRetryIntervalMillis,
                    nextRetryIntervalMillis >= 0 && nextRetryIntervalMillis <= maxRetryIntervalMillis);
        }
    }
}
