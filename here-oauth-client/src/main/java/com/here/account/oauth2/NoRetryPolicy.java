package com.here.account.oauth2;

import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;

import java.io.IOException;


/**
 * An implementation of {@code RetryPolicy} with default behaviour as no retry.
 */
public class NoRetryPolicy implements RetryPolicy {
    @Override
    public HttpProvider.HttpResponse executeWithRetry(HttpProvider httpProvider, HttpProvider.HttpRequest httpRequest) throws HttpException, IOException {
        return httpProvider.execute(httpRequest);
    }
}
