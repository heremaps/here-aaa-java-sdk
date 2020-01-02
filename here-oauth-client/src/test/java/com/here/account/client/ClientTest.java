/*
 * Copyright (c) 2018 HERE Europe B.V.
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
package com.here.account.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.oauth2.AccessTokenException;
import com.here.account.oauth2.ErrorResponse;
import com.here.account.oauth2.RequestExecutionException;
import com.here.account.oauth2.ResponseParsingException;
import com.here.account.oauth2.retry.ExponentialRandomBackOffPolicy;
import com.here.account.olp.OlpHttpMessage;
import com.here.account.util.CloseUtil;
import com.here.account.util.JacksonSerializer;
import com.here.account.util.Serializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class FakeResponse implements OlpHttpMessage {
    @JsonProperty("access_token")
    private final String accessToken;
    @JsonProperty("token_type")
    private final String tokenType;

    private String correlationId;

    public FakeResponse() {
        this(null, null);
    }

    public FakeResponse(String accessToken, String tokenType) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public String getCorrelationId() { return this.correlationId; }
    public OlpHttpMessage setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
}

class FakeRequest {
    @JsonProperty
    private final String clientId;

    @JsonProperty
    private final String scope;

    @JsonProperty
    private final String grantType;

    public FakeRequest(String clientId, String scope, String grantType) {
        this.clientId = clientId;
        this.scope = scope;
        this.grantType = grantType;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getRequestField() {
        return this.scope;
    }

    public String getGrantType() {
        return this.grantType;
    }
}

public class ClientTest {
    private static String expectedCorrelationId = "testCorrId";
    Serializer serializer = new JacksonSerializer();
    HttpProvider.HttpRequest mockHttpRequest;
    HttpProvider.HttpResponse mockHttpResponse;
    Map<String, List<String>> mockResponseHeader;
    HttpProvider mockHttpProvider;
    HttpProvider.HttpRequestAuthorizer mockHttpRequestAuthorizer;
    FakeResponse expectedResponseObject;

    @Before
    public void setUp() throws IOException, HttpException {
        mockHttpRequest = mock(HttpProvider.HttpRequest.class);
        mockHttpProvider = mock(HttpProvider.class);
        mockHttpRequestAuthorizer = mock(HttpProvider.HttpRequestAuthorizer.class);
        mockHttpResponse = mock(HttpProvider.HttpResponse.class);
        List<String> mockCorrelationIdHeaderValue = new ArrayList<String>();
        mockCorrelationIdHeaderValue.add(expectedCorrelationId);
        mockResponseHeader = new HashMap<String, List<String>>();
        mockResponseHeader.put(OlpHttpMessage.X_CORRELATION_ID, mockCorrelationIdHeaderValue);
        expectedResponseObject = new FakeResponse("testAccessToken", "Bearer");
        expectedResponseObject.setCorrelationId(expectedCorrelationId);
        String responseString = serializer.objectToJson(expectedResponseObject);
        InputStream inputStream = new ByteArrayInputStream(responseString.getBytes("UTF-8"));
        Mockito.when(mockHttpResponse.getResponseBody()).thenReturn(inputStream);
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(200);
        Mockito.when(mockHttpResponse.getHeaders()).thenReturn(createMockResponseHeader());
        Mockito.when(mockHttpProvider.execute(mockHttpRequest)).thenReturn(mockHttpResponse);
        Mockito.when(mockHttpProvider.getRequest(Mockito.any(HttpProvider.HttpRequestAuthorizer.class), anyString(), anyString(), anyString()))
                .thenReturn(mockHttpRequest);
    }

    private Map<String, List<String>> createMockResponseHeader() {
        Map<String, List<String>> responseHeader = new HashMap<String, List<String>>();
        List<String> responseTypes = new ArrayList<String>();
        responseTypes.add(HttpConstants.CONTENT_TYPE_JSON);
        responseHeader.put(HttpConstants.CONTENT_TYPE, responseTypes);
        return responseHeader;
    }

    @Test
    public void test_sendMessage2() {
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer).build();
        FakeResponse actualResponse = client.sendMessage(mockHttpRequest, FakeResponse.class,
                ErrorResponse.class, (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
        assertTrue(expectedResponseObject.getAccessToken().equals(actualResponse.getAccessToken()));
        assertTrue(expectedResponseObject.getTokenType().equals(actualResponse.getTokenType()));
    }

    // this test was previously catching a NullPointerException, wrapping it in a thrown ResponseParsingException,
    // as a result of the the HttpResponse having no Content-Type header, and Client lacking the null-check.
    @Test(expected = AccessTokenException.class)
    public void test_sendMessage2_error() throws UnsupportedEncodingException, IOException, HttpException {
        HttpProvider.HttpResponse mockHttpResponse = mock(HttpProvider.HttpResponse.class);
        ErrorResponse expectedErrorResponse = new ErrorResponse("testError", "testErrorDesc",
                "testId", 401, 401100, "testErrorMsg");
        String responseString = serializer.objectToJson(expectedErrorResponse);
        InputStream inputStream = new ByteArrayInputStream(responseString.getBytes("UTF-8"));
        Mockito.when(mockHttpResponse.getResponseBody()).thenReturn(inputStream);
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(401);
        Mockito.when(mockHttpProvider.execute(mockHttpRequest)).thenReturn(mockHttpResponse);

        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer).build();
        try {

            client.sendMessage(mockHttpRequest, FakeResponse.class,
                    ErrorResponse.class, (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);
                    });
            fail("should have thrown exception, but didn't");
        } catch (AccessTokenException e) {
            ErrorResponse actualErrorResponse = e.getErrorResponse();
            assertTrue("errorResponse was expected " + expectedErrorResponse + ", actual " + actualErrorResponse,
                expectedErrorResponse.equals(actualErrorResponse));
            throw e;
        }
    }

    @Test(expected = ResponseParsingException.class)
    public void test_sendMessage2_parsingError() throws IOException, HttpException {
        HttpProvider.HttpResponse mockHttpResponse = mock(HttpProvider.HttpResponse.class);
        InputStream inputStream = new ByteArrayInputStream("{accessToken:}".getBytes("UTF-8"));
        Mockito.when(mockHttpResponse.getResponseBody()).thenReturn(inputStream);
        Mockito.when(mockHttpProvider.execute(mockHttpRequest)).thenReturn(mockHttpResponse);

        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer).build();
        client.sendMessage(mockHttpRequest, FakeResponse.class,
                ErrorResponse.class, (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
    }

    @Test(expected = RequestExecutionException.class)
    public void test_sendMessage2_requestExceutionException() throws IOException, HttpException {
        Mockito.when(mockHttpProvider.execute(mockHttpRequest)).thenThrow(new HttpException("Http Exception"));

        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer).build();
        client.sendMessage(mockHttpRequest, FakeResponse.class,
                ErrorResponse.class, (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
    }

    @Test
    public void test_sendMessage1() {
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(201);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer)
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeRequest fakeRequest = new FakeRequest("testClientId", "testScope", "testGrantType");
        FakeResponse actualResponse = client.sendMessage("POST",
                "http://test.com",
                fakeRequest,
                FakeResponse.class,
                ErrorResponse.class,
                (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
        assertTrue(expectedResponseObject.getAccessToken().equals(actualResponse.getAccessToken()));
        assertTrue(expectedResponseObject.getTokenType().equals(actualResponse.getTokenType()));
        assertTrue(expectedResponseObject.getCorrelationId().equals(actualResponse.getCorrelationId()));
    }

    @Test
    public void test_getClientAuthorizer() {
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(201);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer)
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        HttpProvider.HttpRequestAuthorizer clientAuthorizer = client.getClientAuthorizer();
        assertTrue("expected clientAuthorizer " + mockHttpRequestAuthorizer
                + ", actual " + clientAuthorizer,
                mockHttpRequestAuthorizer == clientAuthorizer);
    }

    @Test
    public void test_sendMessage1_additionalHeaders() {
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(201);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer)
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeRequest fakeRequest = new FakeRequest("testClientId", "testScope", "testGrantType");
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        String additionalHeaderName = "foo";
        String additionalHeaderValue = "bar";
        additionalHeaders.put(additionalHeaderName, additionalHeaderValue);
        FakeResponse actualResponse = client.sendMessage("POST",
                "http://test.com",
                fakeRequest,
                additionalHeaders,
                FakeResponse.class,
                ErrorResponse.class,
                (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
        assertTrue(expectedResponseObject.getAccessToken().equals(actualResponse.getAccessToken()));
        assertTrue(expectedResponseObject.getTokenType().equals(actualResponse.getTokenType()));
        Mockito.verify(mockHttpRequest, Mockito.times(1)).addHeader(additionalHeaderName, additionalHeaderValue);

    }

    @Test
    public void test_sendMessageRetry_500() throws IOException, HttpException {
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(500);
        ExponentialRandomBackOffPolicy exponentialRandomBackoffSpy = spy(ExponentialRandomBackOffPolicy.class);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer).withRetryPolicy(exponentialRandomBackoffSpy)
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeRequest fakeRequest = new FakeRequest("testClientId", "testScope", "testGrantType");
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        String additionalHeaderName = "foo";
        String additionalHeaderValue = "bar";
        additionalHeaders.put(additionalHeaderName, additionalHeaderValue);
        try {
            client.sendMessage("POST",
                    "http://test.com",
                    fakeRequest,
                    additionalHeaders,
                    FakeResponse.class,
                    ErrorResponse.class,
                    (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);
                    });
        } catch (Exception ex) {
            //do nothing
        }

        Mockito.verify(mockHttpProvider, Mockito.times(4)).execute(any(HttpProvider.HttpRequest.class));
    }

    @Test
    public void test_sendMessageRetry_exception() throws IOException, HttpException {

        Mockito.when(mockHttpProvider.execute(mockHttpRequest)).thenThrow(SocketTimeoutException.class);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer).withRetryPolicy(new ExponentialRandomBackOffPolicy())
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeRequest fakeRequest = new FakeRequest("testClientId", "testScope", "testGrantType");
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        String additionalHeaderName = "foo";
        String additionalHeaderValue = "bar";
        additionalHeaders.put(additionalHeaderName, additionalHeaderValue);
        try {
            client.sendMessage("POST",
                    "http://test.com",
                    fakeRequest,
                    additionalHeaders,
                    FakeResponse.class,
                    ErrorResponse.class,
                    (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);
                    });
        } catch (Exception ex) {
            //do nothing
        }

        Mockito.verify(mockHttpProvider, Mockito.times(4)).execute(any(HttpProvider.HttpRequest.class));
    }

    @Test
    public void test_sendMessage_retry_exception_success() throws IOException, HttpException {

        Mockito.when(mockHttpProvider.execute(mockHttpRequest)).thenThrow(SocketTimeoutException.class)
                .thenThrow(SocketTimeoutException.class).thenReturn(mockHttpResponse);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer).withRetryPolicy(new ExponentialRandomBackOffPolicy())
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeRequest fakeRequest = new FakeRequest("testClientId", "testScope", "testGrantType");
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        String additionalHeaderName = "foo";
        String additionalHeaderValue = "bar";
        additionalHeaders.put(additionalHeaderName, additionalHeaderValue);
        try {
            client.sendMessage("POST",
                    "http://test.com",
                    fakeRequest,
                    additionalHeaders,
                    FakeResponse.class,
                    ErrorResponse.class,
                    (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);
                    });
        } catch (Exception ex) {
            //do nothing
        }

        Mockito.verify(mockHttpProvider, Mockito.times(3)).execute(any(HttpProvider.HttpRequest.class));
    }

    @Test
    public void test_sendMessage_retry_500_success() throws IOException, HttpException {
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(500).thenReturn(500).thenReturn(200);
        Mockito.when(mockHttpProvider.execute(mockHttpRequest)).thenReturn(mockHttpResponse);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer).withRetryPolicy(new ExponentialRandomBackOffPolicy())
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeRequest fakeRequest = new FakeRequest("testClientId", "testScope", "testGrantType");
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        String additionalHeaderName = "foo";
        String additionalHeaderValue = "bar";
        additionalHeaders.put(additionalHeaderName, additionalHeaderValue);
        try {
            client.sendMessage("POST",
                    "http://test.com",
                    fakeRequest,
                    additionalHeaders,
                    FakeResponse.class,
                    ErrorResponse.class,
                    (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);
                    });
        } catch (Exception ex) {
            //do nothing
        }

        Mockito.verify(mockHttpProvider, Mockito.times(3)).execute(any(HttpProvider.HttpRequest.class));
    }

    @Test
    public void test_sendMessage_retry_500_exception_success() throws IOException, HttpException {
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(500).thenReturn(200);
        Mockito.when(mockHttpProvider.execute(mockHttpRequest)).thenThrow(SocketTimeoutException.class).thenReturn(mockHttpResponse);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer).withRetryPolicy(new ExponentialRandomBackOffPolicy())
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeRequest fakeRequest = new FakeRequest("testClientId", "testScope", "testGrantType");
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        String additionalHeaderName = "foo";
        String additionalHeaderValue = "bar";
        additionalHeaders.put(additionalHeaderName, additionalHeaderValue);
        try {
            client.sendMessage("POST",
                    "http://test.com",
                    fakeRequest,
                    additionalHeaders,
                    FakeResponse.class,
                    ErrorResponse.class,
                    (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);
                    });
        } catch (Exception ex) {
            //do nothing
        }

        Mockito.verify(mockHttpProvider, Mockito.times(3)).execute(any(HttpProvider.HttpRequest.class));
    }

    @Test
    public void test_sendMessage1_GET_404() throws IOException {
        method = "GET";
        verify404();
    }


    String method;

    @Test
    public void test_sendMessage1_POST_404() throws IOException {
        method = "POST";
        verify404();
    }

    protected void verify404() throws IOException {
        // String error,
        //            String errorDescription,
        //          String errorId,
        //          Integer httpStatus,
        //          Integer errorCode,
        //          String message
        Integer httpStatus = 404;
        Integer errorCode = 4040404;
        expectedResponseObject = null;
        ErrorResponse errorResponse1 = new ErrorResponse("error", "errorDescription", "errorId", httpStatus, errorCode, "message");
        String responseString = serializer.objectToJson(errorResponse1);
        InputStream inputStream = new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8));
        Mockito.when(mockHttpResponse.getResponseBody()).thenReturn(inputStream);

        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(404);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer)
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeRequest fakeRequest = new FakeRequest("testClientId", "testScope", "testGrantType");

        try {
            client.sendMessage(method,
                    "http://test.com",
                    fakeRequest,
                    FakeResponse.class,
                    ErrorResponse.class,
                    (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);
                    });
            fail("should have thrown exception");
        } catch (AccessTokenException e) {
            int expectedStatusCode = 404;
            int statusCode = e.getStatusCode();
            assertTrue("expected status code " + expectedStatusCode + ", actual " + statusCode,
                expectedStatusCode == statusCode);
            ErrorResponse errorResponse2 = e.getErrorResponse();
            assertTrue("errorResponse was expected " + errorResponse1 + ", actual " + errorResponse2,
                    errorResponse1.toString().equals(errorResponse2.toString()));
        }

    }



    @Test
    public void test_sendMessage1_requestBodyNull_204_nobody() throws IOException {
        expectedResponseObject = null;
        InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        Mockito.when(mockHttpResponse.getResponseBody()).thenReturn(inputStream);

        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(204);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer)
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        Void actualResponse = client.sendMessage("POST",
                "http://test.com",
                null,
                Void.class,
                ErrorResponse.class,
                (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
        assertTrue(actualResponse == null);
    }

    @Test(expected = ResponseParsingException.class)
    public void test_sendMessage1_requestBodyNull_204_nobody_notVoid() throws IOException {
        expectedResponseObject = null;
        InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        Mockito.when(mockHttpResponse.getResponseBody()).thenReturn(inputStream);

        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(204);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer)
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeResponse actualResponse = client.sendMessage("POST",
                "http://test.com",
                null,
                FakeResponse.class,
                ErrorResponse.class,
                (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
        assertTrue(actualResponse == null);
    }


    @Test
    public void test_sendMessage1_requestBodyNull_204() {
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(204);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer)
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        Void actualResponse = client.sendMessage("POST",
                "http://test.com",
                null,
                Void.class,
                ErrorResponse.class,
                (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
        assertTrue(actualResponse == null);
    }


    @Test
    public void test_sendMessage1_requestBodyNull() {
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(200);
        Client client = Client.builder().withHttpProvider(mockHttpProvider).withSerializer(serializer)
                .withClientAuthorizer(mockHttpRequestAuthorizer).build();
        FakeResponse actualResponse = client.sendMessage("POST",
                "http://test.com",
                null,
                FakeResponse.class,
                ErrorResponse.class,
                (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
        assertTrue(expectedResponseObject.getAccessToken().equals(actualResponse.getAccessToken()));
        assertTrue(expectedResponseObject.getTokenType().equals(actualResponse.getTokenType()));
    }




    @Test
    public void test_nullSafeCloseThrowingUnchecked_null() {
        CloseUtil.nullSafeCloseThrowingUnchecked(null);
    }

    @Test
    public void test_nullSafeCloseThrowingUnchecked_noException() {
        Closeable closeable = new Closeable() {

            @Override
            public void close() throws IOException {
                // no exceptions thrown
            }
        };
        CloseUtil.nullSafeCloseThrowingUnchecked(closeable);
    }

    @Test
    public void test_nullSafeCloseThrowingUnchecked_withException() {
        final String message = "test I/O trouble!";
        Closeable closeable = new Closeable() {

            @Override
            public void close() throws IOException {
                throw new IOException(message);
            }

        };
        try {
            CloseUtil.nullSafeCloseThrowingUnchecked(closeable);
            Assert.fail("should have thrown UncheckedIOException");
        } catch (UncheckedIOException unchecked) {
            IOException ioe = unchecked.getCause();
            assertTrue("ioe was null", null != ioe);
            String actualMessage = ioe.getMessage();
            assertTrue("message was expected " + message + ", actual " + actualMessage,
                    message.equals(actualMessage));
        }
    }

    @Test
    public void test_response_correlationId() {

    }
}
