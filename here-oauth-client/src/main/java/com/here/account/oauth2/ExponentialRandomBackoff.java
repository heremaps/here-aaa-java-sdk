package com.here.account.oauth2;

import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * {@code ExponentialRandomBackoff} is implementation of {@code RetryPolicy}.
 * Http request is retried with exponential random backoff
 * in case of https status code 5XX or {@code SocketTimeoutException }
 */

public class ExponentialRandomBackoff implements RetryPolicy {

    private final int noOfRetries;
    private final int retryIntervalMillis;

    private static final Logger LOGGER = Logger.getLogger(ExponentialRandomBackoff.class.getName());


    public ExponentialRandomBackoff(int retryIntervalMillis, int noOfRetries) {
        this.noOfRetries = noOfRetries;
        this.retryIntervalMillis = retryIntervalMillis;
    }

    public ExponentialRandomBackoff() {
        noOfRetries = 3;
        retryIntervalMillis = 1000;

    }

    @Override
    public HttpProvider.HttpResponse executeWithRetry(HttpProvider httpProvider, HttpProvider.HttpRequest httpRequest) throws HttpException, IOException {
        return callWithRetry(httpProvider, httpRequest, noOfRetries);
    }

    private HttpProvider.HttpResponse callWithRetry(HttpProvider httpProvider, HttpProvider.HttpRequest httpRequest, int noOfRetriesLeft) throws HttpException, IOException {
        HttpProvider.HttpResponse httpResponse;
        try {
            httpResponse =  httpProvider.execute(httpRequest);
        } catch (SocketTimeoutException ex) {
            if (noOfRetriesLeft > 0) {
                try {
                    int waitTime = getRetryIntervalMillis(noOfRetriesLeft);
                    LOGGER.info("Got SocketTimeoutException. Retrying after "+waitTime+" millis");
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {

                }
                return callWithRetry(httpProvider, httpRequest, noOfRetriesLeft - 1);
            }
            else {
                throw ex;// when retry limit is exhausted, throw the exception as is.
            }
        }

        if (httpResponse.getStatusCode() >= 500) {
            if (noOfRetriesLeft > 0) {
                try {
                    int waitTime = getRetryIntervalMillis(noOfRetriesLeft);
                    LOGGER.info("Got http status code - 5XX. Retrying after "+waitTime+" millis");
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {

                }
                return callWithRetry(httpProvider, httpRequest, noOfRetriesLeft - 1);
            }
            else {
                return httpResponse;// when retry limit is exhausted, return the response as is.
            }
        }
        else {
            return httpResponse;
        }
    }

    private int getRetryIntervalMillis(int noOfRetriesLeft) {
        int factor = 1 << (noOfRetries - noOfRetriesLeft);
        return retryIntervalMillis + retryIntervalMillis * ThreadLocalRandom.current().nextInt(factor);
    }
}
