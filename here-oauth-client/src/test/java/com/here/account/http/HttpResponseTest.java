package com.here.account.http;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class HttpResponseTest {

    @Test
    public void test_backwardcompatible_HttpResponse_getHeaders() {
        final String bodyString = "bodyString";
        final byte[] bodyBytes = bodyString.getBytes(StandardCharsets.UTF_8);
        final int statusCode = 123;
        final int contentLength = bodyBytes.length;
        HttpProvider.HttpResponse httpResponse = new HttpProvider.HttpResponse() {

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public long getContentLength() {
                return contentLength;
            }

            @Override
            public InputStream getResponseBody() throws IOException {
                return new ByteArrayInputStream(bodyBytes);
            }
        };

        Map<String, List<String>> headers = httpResponse.getHeaders();
        assertTrue("headers was null", null != headers);
        assertTrue("headers was not empty", headers.isEmpty());
    }
}
