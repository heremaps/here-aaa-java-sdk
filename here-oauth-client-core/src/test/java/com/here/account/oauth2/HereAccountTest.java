/*
 * Copyright (c) 2016 HERE Europe B.V.
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
package com.here.account.oauth2;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpResponse;
import com.here.account.identity.bo.IdentityTokenRequest;
import com.here.account.util.Clock;
import com.here.account.util.JacksonSerializer;
import com.here.account.util.SettableSystemClock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HereAccountTest extends AbstractCredentialTezt {
    
    @Test(expected=NullPointerException.class)
    public void testGetTokenNullUrl() throws Exception {
        HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider(null, accessKeyId, accessKeySecret));
    }
    
    @Test(expected=NullPointerException.class)
    public void testGetTokenNullAccessKeyId() throws Exception {
        HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider(url, null, accessKeySecret));
    }
    
    @Test(expected=NullPointerException.class)
    public void testGetTokenNullAccessKeySecret() throws Exception {
        HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, null));
    }
    
    @Test
    public void testGetTokenInvalidUrl() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider("bogus", accessKeyId, accessKeySecret));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected RequestExecutionException");
        } catch (RequestExecutionException ree) {
            
        }
    }
    
    /**
     * Confirms MissingRequiredParameter => AccessTokenException whose 
     * ErrorResponse object has error="invalid_request", so clients 
     * could potentially write code against the RFC6749 using these 
     * business objects.
     * 
     * @throws Exception if an unexpected Exception is thrown by the test.
     */
    @Test
    @Ignore // TODO: un-Ignore.  integration test fails for now, needs server-side fix to re-activate
    public void testGetToken_MissingRequiredParameter() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        
        AccessTokenRequest missingParameterRequest = new AccessTokenRequest(null) {

            @Override
            public String toJson() {
                return "{}";
            }

            @Override
            public Map<String, List<String>> toFormParams() {
                Map<String, List<String>> formParams = new HashMap<String, List<String>>();
                //addFormParam(formParams, "grant_type", getGrantType());
                return formParams;
            }
            
        };
        try {
            tokenEndpoint.requestToken(missingParameterRequest);
            Assert.fail("Expected AccessTokenException");
        } catch (AccessTokenException ate) {
            ErrorResponse errorResponse = ate.getErrorResponse();
            assertTrue("errorResponse was null", null != errorResponse);
            String error = errorResponse.getError();
            final String expectedError = "invalid_request";
            assertTrue("\"error\" in JSON error response body was expected "
                    +expectedError+", actual "+error, 
                    expectedError.equals(error));
        }
    }

    
    @Test
    public void testGetTokenInvalidResponseBody() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                mockHttpProvider(dummyResponse(200, 
                                               "bogus".getBytes().length, 
                                               new ByteArrayInputStream("bogus".getBytes("UTF-8")))),
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected ResponseParsingException");
        } catch (ResponseParsingException rpe) {
            
        }
    }
    
    @Test
    public void testGetTokenInvalidErrorResponse() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                mockHttpProvider(dummyResponse(400, 
                                               "bogus".getBytes().length, 
                                               new ByteArrayInputStream("bogus".getBytes("UTF-8")))),
                new OAuth1ClientCredentialsProvider(url, accessKeyId, "invalidSecret"));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected ResponseParsingException");
        } catch (ResponseParsingException rpe) {
            
        }
    }
    
    @Test
    public void testGetTokenValidErrorResponse() throws Exception {
        final String error = "unauthorized_client";
        final String responseBody = "{\"error\":\""+error+"\"}";
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                mockHttpProvider(dummyResponse(400, 
                                               responseBody.getBytes().length, 
                                               new ByteArrayInputStream(responseBody.getBytes("UTF-8")))),
                new OAuth1ClientCredentialsProvider(url, accessKeyId, "mySecret"));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected AccessTokenException");
        } catch (AccessTokenException e) {
            ErrorResponse errorResponse = e.getErrorResponse();
            assertTrue("errorResponse was null", null != errorResponse);
            String actualError = errorResponse.getError();
            assertTrue("error was expected "+error+", actual "+actualError, 
                    error.equals(actualError));
        }
    }

    @Test
    public void test_getNewAccessTokenRequest() {
        OAuth1ClientCredentialsProvider clientAuthorizationRequestProvider = new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret);
        Assert.assertThat(clientAuthorizationRequestProvider.getNewAccessTokenRequest(), instanceOf(AccessTokenRequest.class));
    }

    @Test
    public void test_nullSafeCloseThrowingUnchecked_null() {
        HereAccount.nullSafeCloseThrowingUnchecked(null);
    }

    @Test
    public void test_nullSafeCloseThrowingUnchecked_noException() {
        Closeable closeable = new Closeable() {

            @Override
            public void close() throws IOException {
                // no exceptions thrown
            }
            
        };
        HereAccount.nullSafeCloseThrowingUnchecked(closeable);
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
            HereAccount.nullSafeCloseThrowingUnchecked(closeable);
            Assert.fail("should have thrown UncheckedIOException");
        } catch (UncheckedIOException unchecked) {
            IOException ioe = unchecked.getCause();
            assertTrue("ioe was null", null != ioe);
            String actualMessage = ioe.getMessage();
            assertTrue("message was expected "+message+", actual "+actualMessage, 
                    message.equals(actualMessage));
        }
    }


    @Test
    public void testGetTokenHttpExceptionExecuting() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                mockThrowingHttpProvider(new HttpException("error")),
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected RequestExecutionException");
        } catch (RequestExecutionException ree) {
            
        }
    }
    
    @Test
    public void testGetTokenIOExceptionExecuting() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                mockThrowingHttpProvider(new IOException("error")),
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected RequestExecutionException");
        } catch (RequestExecutionException ree) {
            
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_getTokenEndpoint_null_clientAuthorizationRequestProvider() {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        HereAccount.getTokenEndpoint(mockHttpProvider, null);
    }

    @Test(expected = NullPointerException.class)
    public void test_getTokenEndpoint_null_url() {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        ClientAuthorizationRequestProvider mockClientAuthorizationRequestProvider =
                Mockito.mock(ClientAuthorizationRequestProvider.class);
        Mockito.doReturn(null)
                .when(mockClientAuthorizationRequestProvider).getTokenEndpointUrl();
        Mockito.doReturn(HttpConstants.HttpMethods.POST)
                .when(mockClientAuthorizationRequestProvider).getHttpMethod();

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider, mockClientAuthorizationRequestProvider);
        assertTrue("tokenEndpoint was null", null != tokenEndpoint);

        AccessTokenRequest accessTokenRequest = new IdentityTokenRequest();
        // we will get an error as the apache http response is null.
        tokenEndpoint.requestToken(accessTokenRequest);
    }

    @Test
    public void test_handleFixableErrors_timestampResponseError() throws IOException, HttpException {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);

        int statusCode = 401;
        String error = "foo";
        String errorDescription = "bar";
        String errorId = "3";
        Integer httpStatus = statusCode;
        Integer errorCode = 401204;
        String message = "none";
        ErrorResponse errorResponse = new ErrorResponse(
                error,
                errorDescription,
                errorId,
                httpStatus,
                errorCode,
                message
        );

        AccessTokenException toBeThrown = new AccessTokenException( statusCode, errorResponse);

        int timestampStatusCode = 404;
        Integer timestampErrorCode = 40404;
        HttpResponse timestampHttpResponse = Mockito.mock(HttpResponse.class);
        String timestampResponseBody = "{\"errorCode\":" + timestampErrorCode + "}";
        byte[] timestampBytes = timestampResponseBody.getBytes(StandardCharsets.UTF_8);

        Mockito.doReturn(timestampStatusCode)
                .when(timestampHttpResponse).getStatusCode();
        Mockito.doReturn(((long) timestampBytes.length))
                .when(timestampHttpResponse).getContentLength();
        Mockito.doReturn(new ByteArrayInputStream(timestampBytes))
                .when(timestampHttpResponse).getResponseBody();

        /*ErrorResponse timestampErrorResponse = new ErrorResponse(
                error,
                errorDescription,
                errorId,
                timestampStatusCode,
                timestampErrorCode,
                message
        );*/

        Mockito.when(mockHttpProvider.execute(Mockito.any(HttpProvider.HttpRequest.class)))
                .thenThrow(toBeThrown)
                .thenReturn(timestampHttpResponse);

        ClientAuthorizationRequestProvider mockClientAuthorizationRequestProvider =
                Mockito.mock(ClientAuthorizationRequestProvider.class);
        Mockito.doReturn("https://www.example.com/oauth2/token")
                .when(mockClientAuthorizationRequestProvider).getTokenEndpointUrl();
        Mockito.doReturn(HttpConstants.HttpMethods.POST)
                .when(mockClientAuthorizationRequestProvider).getHttpMethod();


        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider, mockClientAuthorizationRequestProvider);
        assertTrue("tokenEndpoint was null", null != tokenEndpoint);

        AccessTokenRequest accessTokenRequest = new IdentityTokenRequest();
        // we will get an error as the apache http response is null.
        try {
            tokenEndpoint.requestToken(accessTokenRequest);
        } catch (AccessTokenException e) {
            // we expect the error from the original AccessTokenRequest to propagate.
            // the timestamp error is suppressed
            verifyExpected(e, 401, 401204);
        }

        Mockito.verify(mockHttpProvider, Mockito.times(2))
                .execute(Mockito.any(HttpProvider.HttpRequest.class));
    }

    protected void verifyExpected(AccessTokenException e, int expectedStatusCode, int expectedErrorCode) {
        int statusCode2 = e.getStatusCode();
        assertTrue("statusCode2 was expected " + expectedStatusCode + ", actual " + statusCode2,
                expectedStatusCode == statusCode2);
        ErrorResponse errorResponse2 = e.getErrorResponse();
        assertTrue("errorResponse2 was null", null != errorResponse2);
        int errorCode2 = errorResponse2.getErrorCode();
        assertTrue("errorCode2 was expected " + expectedErrorCode + ", actual " + errorCode2,
                expectedErrorCode == errorCode2);
    }

    @Test
    public void test_handleFixableErrors_401204_twice() throws IOException, HttpException {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);

        HttpResponse timestampHttpResponse = Mockito.mock(HttpResponse.class);
        String timestampResponseBody = "{\"timestamp\":123}";
        byte[] timestampBytes = timestampResponseBody.getBytes(StandardCharsets.UTF_8);

        Mockito.doReturn(200)
                .when(timestampHttpResponse).getStatusCode();
        Mockito.doReturn(((long) timestampBytes.length))
                .when(timestampHttpResponse).getContentLength();
        Mockito.doReturn(new ByteArrayInputStream(timestampBytes))
                .when(timestampHttpResponse).getResponseBody();


        HttpResponse tokenHttpResponse = Mockito.mock(HttpResponse.class);
        String expectedAccessToken = "abc."+UUID.randomUUID().toString()+".xyz";
        String responseBody = getResponseBody(expectedAccessToken);
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);

        Mockito.doReturn(200)
                .when(tokenHttpResponse).getStatusCode();
        Mockito.doReturn(((long) bytes.length))
                .when(tokenHttpResponse).getContentLength();
        Mockito.doReturn(new ByteArrayInputStream(bytes))
                .when(tokenHttpResponse).getResponseBody();

        int statusCode = 401;
        String error = "foo";
        String errorDescription = "bar";
        String errorId = "3";
        Integer httpStatus = statusCode;
        Integer errorCode = 401204;
        String message = "none";
        ErrorResponse errorResponse = new ErrorResponse(
                error,
                errorDescription,
                errorId,
                httpStatus,
                errorCode,
                message
        );

        AccessTokenException toBeThrown = new AccessTokenException( statusCode, errorResponse);

        Mockito.when(mockHttpProvider.execute(Mockito.any(HttpProvider.HttpRequest.class)))
                .thenThrow(toBeThrown)
                .thenReturn(timestampHttpResponse)
                .thenThrow(toBeThrown);

        /*Mockito.doThrow(toBeThrown)
                .when(mockHttpProvider).execute(Mockito.any(HttpProvider.HttpRequest.class));*/
        /*Mockito.doReturn(httpResponse)
                .when(mockHttpProvider).execute(Mockito.any(HttpProvider.HttpRequest.class));*/
        ClientAuthorizationRequestProvider mockClientAuthorizationRequestProvider =
                Mockito.mock(ClientAuthorizationRequestProvider.class);
        Mockito.doReturn("https://www.example.com/oauth2/token")
                .when(mockClientAuthorizationRequestProvider).getTokenEndpointUrl();
        Mockito.doReturn(HttpConstants.HttpMethods.POST)
                .when(mockClientAuthorizationRequestProvider).getHttpMethod();


        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider, mockClientAuthorizationRequestProvider);
        assertTrue("tokenEndpoint was null", null != tokenEndpoint);

        try {
            AccessTokenRequest accessTokenRequest = new IdentityTokenRequest();
            tokenEndpoint.requestToken(accessTokenRequest);
            fail("expected exception");
        } catch (AccessTokenException e) {
            verifyExpected(e, 401, 401204);
        }

        Mockito.verify(mockHttpProvider, Mockito.times(3))
                .execute(Mockito.any(HttpProvider.HttpRequest.class));
    }


    @Test
    public void test_handleFixableErrors() throws IOException, HttpException {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);

        HttpResponse timestampHttpResponse = Mockito.mock(HttpResponse.class);
        String timestampResponseBody = "{\"timestamp\":123}";
        byte[] timestampBytes = timestampResponseBody.getBytes(StandardCharsets.UTF_8);

        Mockito.doReturn(200)
                .when(timestampHttpResponse).getStatusCode();
        Mockito.doReturn(((long) timestampBytes.length))
                .when(timestampHttpResponse).getContentLength();
        Mockito.doReturn(new ByteArrayInputStream(timestampBytes))
                .when(timestampHttpResponse).getResponseBody();


        HttpResponse tokenHttpResponse = Mockito.mock(HttpResponse.class);
        String expectedAccessToken = "abc."+UUID.randomUUID().toString()+".xyz";
        String responseBody = getResponseBody(expectedAccessToken);
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);

        Mockito.doReturn(200)
                .when(tokenHttpResponse).getStatusCode();
        Mockito.doReturn(((long) bytes.length))
                .when(tokenHttpResponse).getContentLength();
        Mockito.doReturn(new ByteArrayInputStream(bytes))
                .when(tokenHttpResponse).getResponseBody();

        int statusCode = 401;
        String error = "foo";
        String errorDescription = "bar";
        String errorId = "3";
        Integer httpStatus = statusCode;
        Integer errorCode = 401204;
        String message = "none";
        ErrorResponse errorResponse = new ErrorResponse(
                 error,
                 errorDescription,
                 errorId,
                 httpStatus,
                 errorCode,
                 message
        );

        AccessTokenException toBeThrown = new AccessTokenException( statusCode, errorResponse);

        Mockito.when(mockHttpProvider.execute(Mockito.any(HttpProvider.HttpRequest.class)))
                .thenThrow(toBeThrown)
                .thenReturn(timestampHttpResponse)
                .thenReturn(tokenHttpResponse);

        /*Mockito.doThrow(toBeThrown)
                .when(mockHttpProvider).execute(Mockito.any(HttpProvider.HttpRequest.class));*/
        /*Mockito.doReturn(httpResponse)
                .when(mockHttpProvider).execute(Mockito.any(HttpProvider.HttpRequest.class));*/
        ClientAuthorizationRequestProvider mockClientAuthorizationRequestProvider =
                Mockito.mock(ClientAuthorizationRequestProvider.class);
        Mockito.doReturn("https://www.example.com/oauth2/token")
                .when(mockClientAuthorizationRequestProvider).getTokenEndpointUrl();
        Mockito.doReturn(HttpConstants.HttpMethods.POST)
                .when(mockClientAuthorizationRequestProvider).getHttpMethod();


        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider, mockClientAuthorizationRequestProvider);
        assertTrue("tokenEndpoint was null", null != tokenEndpoint);

        AccessTokenRequest accessTokenRequest = new IdentityTokenRequest();
        // we will get an error as the apache http response is null.
        AccessTokenResponse accessTokenResponse = tokenEndpoint.requestToken(accessTokenRequest);
        assertTrue("accessTokenResponse was null", null != accessTokenResponse);
        String accessToken = accessTokenResponse.getAccessToken();
        assertTrue("expected accessToken " + expectedAccessToken + ", actual " + accessToken,
                expectedAccessToken.equals(accessToken));

        Mockito.verify(mockHttpProvider, Mockito.times(3))
                .execute(Mockito.any(HttpProvider.HttpRequest.class));
    }


    @Test
    public void test_requestTokenFromFile() throws IOException {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        ClientAuthorizationRequestProvider mockClientAuthorizationRequestProvider =
                Mockito.mock(ClientAuthorizationRequestProvider.class);

        //  Mockito.doReturn(myAuthorizer).when(mockProvider).getClientAuthorizer();

        File file = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        try {
            final String expectedAccessToken = "ey23.45";
            writeToFile(file, getResponseBody(expectedAccessToken));

            Mockito.doReturn("file://" + file.getAbsolutePath())
                    .when(mockClientAuthorizationRequestProvider).getTokenEndpointUrl();

            TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider,
                    mockClientAuthorizationRequestProvider);

            AccessTokenResponse accessTokenResponse = tokenEndpoint.requestToken(new IdentityTokenRequest());
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            String accessToken = accessTokenResponse.getAccessToken();
            assertTrue("expected access token " + expectedAccessToken + ", actual " + accessToken,
                    expectedAccessToken.equals(accessToken));
        } finally {
            file.delete();
        }

    }

    protected static String getResponseBody(String expectedAccessToken) {
        return "{\"access_token\":\""+expectedAccessToken+"\",\"expires_in\":54321}";
    }

    @Test(expected = RequestExecutionException.class)
    public void test_requestTokenFromFile_ioTrouble() throws IOException {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        ClientAuthorizationRequestProvider mockClientAuthorizationRequestProvider =
                Mockito.mock(ClientAuthorizationRequestProvider.class);

        //  Mockito.doReturn(myAuthorizer).when(mockProvider).getClientAuthorizer();

        File file = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        try {
            final String expectedAccessToken = "ey23.45";
            writeToFile(file, "{\"access_token\":\""+expectedAccessToken+"\"}");

            Mockito.doReturn("file://" + file.getAbsolutePath())
                    .when(mockClientAuthorizationRequestProvider).getTokenEndpointUrl();

            TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider,
                    mockClientAuthorizationRequestProvider);

            file.delete();

            tokenEndpoint.requestToken(new IdentityTokenRequest());
        } finally {
            file.delete();
        }

    }


    private void writeToFile(File file, String content) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    @Test
    public void testGetFreshTokenVerifyRefresh() throws Exception {
        // first token expires after 30 seconds (minimum refresh time)
        String validToken1 = "{"
                + " \"access_token\": \"12345\","
                + " \"expires_in\": 30"
                + "}";
        String validToken2 = "{"
                + " \"access_token\": \"67890\","
                + " \"expires_in\": 30"
                + "}";
        final long sleepTimeMillis = 800L;
            Clock mySettableClock = new SettableSystemClock() {
                @Override
                public void schedule(ScheduledExecutorService scheduledExecutorService,
                                                 Runnable runnable,
                                                 long millisecondsInTheFutureToSchedule
                ) {
                    super.schedule(scheduledExecutorService, runnable, sleepTimeMillis);
                }
            };
        
        TokenEndpoint tokenEndpoint = (TokenEndpoint) HereAccount.getTokenEndpoint(
                mockHttpProvider(dummyResponse(200,
                                               validToken1.getBytes().length, 
                                               new ByteArrayInputStream(validToken1.getBytes("UTF-8"))),
                                 dummyResponse(200,
                                               validToken2.getBytes().length,
                                               new ByteArrayInputStream(validToken2.getBytes("UTF-8")))),
                new OAuth1ClientCredentialsProvider(mySettableClock, url, accessKeyId, accessKeySecret),
                new JacksonSerializer());
        
        Fresh<AccessTokenResponse> freshToken = tokenEndpoint.
                requestAutoRefreshingToken(new ClientCredentialsGrantRequest());
        // verify validToken1
        Assert.assertEquals("12345", freshToken.get().getAccessToken());
        Assert.assertEquals("12345", freshToken.get().getAccessToken());
        // wait for refresh
        Thread.sleep(sleepTimeMillis + 100L);
        // verify validToken2
        Assert.assertEquals("67890", freshToken.get().getAccessToken());
    }
    
    
    private HttpResponse dummyResponse(final int statusCode,
                                       final long contentLength,
                                       final InputStream body) {
        return new HttpResponse() {
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
                return body;
            }
        };
    }
    
    /**
     * Build a mock HttpProvider that always returns the provided response body.
     */
    private HttpProvider mockHttpProvider(HttpResponse... responses) throws Exception {
        HttpProvider mock = Mockito.mock(HttpProvider.class);
        OngoingStubbing<HttpResponse> stub = Mockito.when(mock.execute(Mockito.any()));
        for (HttpResponse response : responses) {
            stub = stub.thenReturn(response);
        }
        return mock;
    }
    
    /**
     * Build a mock HttpProvider that always throws the given exception when
     * attempting to execute the http request.
     */
    private HttpProvider mockThrowingHttpProvider(final Throwable throwable) throws Exception {
        HttpProvider mock = Mockito.mock(HttpProvider.class);
        Mockito.when(mock.execute(Mockito.any())).thenThrow(throwable);
        return mock;
    }
}
