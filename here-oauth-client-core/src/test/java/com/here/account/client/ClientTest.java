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
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.oauth2.AccessTokenException;
import com.here.account.oauth2.ErrorResponse;
import com.here.account.oauth2.RequestExecutionException;
import com.here.account.oauth2.ResponseParsingException;
import com.here.account.util.JacksonSerializer;
import com.here.account.util.Serializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

class FakeResponse {
    @JsonProperty("access_token")
    private final String accessToken;
    @JsonProperty("token_type")
    private final String tokenType;

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

    Serializer serializer = new JacksonSerializer();
    HttpProvider.HttpRequest mockHttpRequest;
    HttpProvider.HttpResponse mockHttpResponse;
    HttpProvider mockHttpProvider;
    HttpProvider.HttpRequestAuthorizer mockHttpRequestAuthorizer;
    FakeResponse expectedResponseObject;

    @Before
    public void setUp() throws IOException, HttpException {
        mockHttpRequest = mock(HttpProvider.HttpRequest.class);
        mockHttpProvider = mock(HttpProvider.class);
        mockHttpRequestAuthorizer = mock(HttpProvider.HttpRequestAuthorizer.class);
        mockHttpResponse = mock(HttpProvider.HttpResponse.class);
        expectedResponseObject = new FakeResponse("testAccessToken", "Bearer");
        String responseString = serializer.objectToJson(expectedResponseObject);
        InputStream inputStream = new ByteArrayInputStream(responseString.getBytes("UTF-8"));
        Mockito.when(mockHttpResponse.getResponseBody()).thenReturn(inputStream);
        Mockito.when(mockHttpResponse.getStatusCode()).thenReturn(200);
        Mockito.when(mockHttpProvider.execute(mockHttpRequest)).thenReturn(mockHttpResponse);
        Mockito.when(mockHttpProvider.getRequest(Mockito.any(HttpProvider.HttpRequestAuthorizer.class), anyString(), anyString(), anyString()))
                .thenReturn(mockHttpRequest);
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
        client.sendMessage(mockHttpRequest, FakeResponse.class,
                ErrorResponse.class, (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
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
    }

    @Test
    public void test_sendMessage1_requestBodyNull() {
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
        assertTrue(expectedResponseObject.getAccessToken().equals(actualResponse.getAccessToken()));
        assertTrue(expectedResponseObject.getTokenType().equals(actualResponse.getTokenType()));
    }

    @Test
    public void test_nullSafeCloseThrowingUnchecked_null() {
        Client.nullSafeCloseThrowingUnchecked(null);
    }

    @Test
    public void test_nullSafeCloseThrowingUnchecked_noException() {
        Closeable closeable = new Closeable() {

            @Override
            public void close() throws IOException {
                // no exceptions thrown
            }
        };
        Client.nullSafeCloseThrowingUnchecked(closeable);
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
            Client.nullSafeCloseThrowingUnchecked(closeable);
            Assert.fail("should have thrown UncheckedIOException");
        } catch (UncheckedIOException unchecked) {
            IOException ioe = unchecked.getCause();
            assertTrue("ioe was null", null != ioe);
            String actualMessage = ioe.getMessage();
            assertTrue("message was expected " + message + ", actual " + actualMessage,
                    message.equals(actualMessage));
        }
    }


}
