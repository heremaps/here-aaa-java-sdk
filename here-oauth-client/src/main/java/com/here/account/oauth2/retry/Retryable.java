package com.here.account.oauth2.retry;

import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;

import java.io.IOException;

/**
 * An interface for an operation that can be retried using a {@link RetryExecutor}
 *
 */
@FunctionalInterface
public interface Retryable {
    /**
     * Execute an operation with retry semantics.
     * @return http response from retry semantics.
     * @throws IOException
     * @throws HttpException
     */
    HttpProvider.HttpResponse execute() throws IOException, HttpException;
}
