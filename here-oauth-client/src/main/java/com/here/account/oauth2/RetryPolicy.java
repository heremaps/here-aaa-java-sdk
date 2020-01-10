package com.here.account.oauth2;

import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;

import java.io.IOException;

/**
 *  A {@code RetryPolicy} lets you retry http get token call.
 */

public interface RetryPolicy {
    HttpProvider.HttpResponse executeWithRetry(HttpProvider httpProvider, HttpProvider.HttpRequest httpRequest)
            throws HttpException, IOException;
}
