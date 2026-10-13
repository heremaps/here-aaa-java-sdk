/*
 * Copyright (c) 2026 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.here.account.oauth2.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import com.here.account.http.HttpProvider;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryExecutorTest {

    private static final int SERVICE_UNAVAILABLE = 503;

    private final List<TrackedResponse> responses = new ArrayList<TrackedResponse>();
    private final AtomicInteger attempts = new AtomicInteger();

    @Test
    public void test_abandoned_responses_are_closed() throws Exception {
        new RetryExecutor(retryUpTo(2)).execute(this::newResponse);

        assertEquals(3, responses.size());
        assertEquals(1, responses.get(0).closes);
        assertEquals(1, responses.get(1).closes);
    }

    @Test
    public void test_returned_response_is_not_closed() throws Exception {
        HttpProvider.HttpResponse returned = new RetryExecutor(retryUpTo(1)).execute(this::newResponse);

        assertSame(responses.get(1), returned);
        assertEquals(0, responses.get(1).closes);
    }

    @Test
    public void test_abandoned_response_is_closed_once() throws Exception {
        try {
            new RetryExecutor(retryUpTo(3)).execute(() -> {
                if (attempts.getAndIncrement() == 0) {
                    return newResponse();
                }
                throw new IOException();
            });
            fail();
        } catch (IOException expected) {
            assertEquals(1, responses.get(0).closes);
        }
    }

    @Test
    public void test_policy_is_given_an_open_response() throws Exception {
        final RetryPolicy retryOnce = retryUpTo(1);
        final List<Integer> closesWhenJudged = new ArrayList<Integer>();

        new RetryExecutor(new RetryPolicy() {
            @Override
            public boolean shouldRetry(RetryContext retryContext) {
                closesWhenJudged.add(((TrackedResponse) retryContext.getLastRetryResponse()).closes);
                return retryOnce.shouldRetry(retryContext);
            }

            @Override
            public int getNextRetryIntervalMillis(RetryContext retryContext) {
                closesWhenJudged.add(((TrackedResponse) retryContext.getLastRetryResponse()).closes);
                return retryOnce.getNextRetryIntervalMillis(retryContext);
            }
        }).execute(this::newResponse);

        assertEquals(3, closesWhenJudged.size());
        for (Integer closes : closesWhenJudged) {
            assertEquals(0, closes.intValue());
        }
    }

    @Test
    public void test_first_attempt_throwing_leaves_nothing_to_release() throws Exception {
        HttpProvider.HttpResponse returned = new RetryExecutor(retryUpTo(1)).execute(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IOException();
            }
            return newResponse();
        });

        assertSame(responses.get(0), returned);
    }

    @Test
    public void test_response_without_a_body_is_released() throws Exception {
        HttpProvider.HttpResponse bodiless = responseWithBody(null);

        assertSame(bodiless, new RetryExecutor(retryUpTo(1)).execute(() -> bodiless));
    }

    @Test
    public void test_failure_to_release_does_not_abort_the_retry() throws Exception {
        HttpProvider.HttpResponse unreleasable = responseWithBody(new ByteArrayInputStream(new byte[0]) {
            @Override
            public void close() throws IOException {
                throw new IOException();
            }
        });

        HttpProvider.HttpResponse returned = new RetryExecutor(retryUpTo(1)).execute(() -> {
            if (attempts.getAndIncrement() == 0) {
                return unreleasable;
            }
            return newResponse();
        });

        assertSame(responses.get(0), returned);
    }

    private TrackedResponse newResponse() {
        TrackedResponse response = new TrackedResponse();
        responses.add(response);
        return response;
    }

    private static HttpProvider.HttpResponse responseWithBody(final InputStream responseBody) {
        return new HttpProvider.HttpResponse() {
            @Override
            public int getStatusCode() {
                return SERVICE_UNAVAILABLE;
            }

            @Override
            public long getContentLength() {
                return 0;
            }

            @Override
            public InputStream getResponseBody() {
                return responseBody;
            }
        };
    }

    private static RetryPolicy retryUpTo(final int maxRetries) {
        return new RetryPolicy() {
            @Override
            public boolean shouldRetry(RetryContext retryContext) {
                return retryContext.getRetryCount() < maxRetries;
            }

            @Override
            public int getNextRetryIntervalMillis(RetryContext retryContext) {
                return 0;
            }
        };
    }

    private static final class TrackedResponse implements HttpProvider.HttpResponse {

        private int closes;

        @Override
        public int getStatusCode() {
            return SERVICE_UNAVAILABLE;
        }

        @Override
        public long getContentLength() {
            return 0;
        }

        @Override
        public InputStream getResponseBody() {
            return new ByteArrayInputStream(new byte[0]) {
                @Override
                public void close() {
                    closes++;
                }
            };
        }
    }
}
