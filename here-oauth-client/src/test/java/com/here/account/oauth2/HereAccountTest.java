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

import com.here.account.auth.NoAuthorizer;
import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.auth.OAuth1Signer;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpResponse;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.http.java.JavaHttpProvider;
import com.here.account.identity.bo.IdentityTokenRequest;
import com.here.account.util.Clock;
import com.here.account.util.JacksonSerializer;
import com.here.account.util.SettableSystemClock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

import static org.mockito.Mockito.times;

public class HereAccountTest extends AbstractCredentialTezt {
    
    @Test(expected=NullPointerException.class)
    public void testGetTokenNullUrl() throws Exception {
        HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider((String)null, accessKeyId, accessKeySecret, scope));
    }
    
    @Test(expected=NullPointerException.class)
    public void testGetTokenNullAccessKeyId() throws Exception {
        HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider(url, null, accessKeySecret, scope));
    }
    
    @Test(expected=NullPointerException.class)
    public void testGetTokenNullAccessKeySecret() throws Exception {
        HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, null, scope));
    }

    @Test
    public void testGetTokenNullScope() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                getHttpProvider(),
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret, null));
        AccessTokenResponse atr = tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
        String actualScope = atr.getScope();
        assertNull("expected scope to be NULL, actual " + actualScope, actualScope);
    }

    @Test
    public void testGetTokenInvalidUrl() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider("bogus", accessKeyId, accessKeySecret, scope));
        
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
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret, scope));
        
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
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret, scope));
        
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
                new OAuth1ClientCredentialsProvider(url, accessKeyId, "invalidSecret", scope));
        
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
                new OAuth1ClientCredentialsProvider(url, accessKeyId, "mySecret", scope));
        
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
        OAuth1ClientCredentialsProvider clientAuthorizationRequestProvider = new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret, scope);
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
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret, scope));
        
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
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret, scope));
        
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

        Mockito.verify(mockHttpProvider, times(2))
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
    public void test_defaultScope_overridden() throws IOException, HttpException {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        //HttpProvider spy = Mockito.spy(mockHttpProvider);

        ClientCredentialsProvider credentials = new ClientCredentialsProvider() {
            @Override
            public String getTokenEndpointUrl() {
                return "https://www.example.com/token";
            }

            @Override
            public HttpProvider.HttpRequestAuthorizer getClientAuthorizer() {
                return new NoAuthorizer();
            }

            @Override
            public AccessTokenRequest getNewAccessTokenRequest() {
                return new ClientCredentialsGrantRequest();
            }

            @Override
            public HttpConstants.HttpMethods getHttpMethod() {
                return HttpConstants.HttpMethods.POST;
            }

            @Override
            public Clock getClock() {
                return Clock.SYSTEM;
            }

            @Override
            public String getScope() {
                return "hrn:here-dev:authorization::rlm00001:project/my-project";
            }
        };

        HttpProvider.HttpResponse mockHttpResponse = Mockito.mock(HttpProvider.HttpResponse.class);
        InputStream inputStream = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
        Mockito.doReturn(200)
                .when(mockHttpResponse).getStatusCode();
        Mockito.doReturn(inputStream)
                .when(mockHttpResponse).getResponseBody();

        Mockito.doReturn(mockHttpResponse)
                .when(mockHttpProvider).execute(Mockito.any(HttpProvider.HttpRequest.class));

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                mockHttpProvider,
                credentials);
        AccessTokenRequest accessTokenRequest  =  new
                ClientCredentialsGrantRequest();
        accessTokenRequest.setScope("openid");
        AccessTokenResponse token =
                tokenEndpoint.requestToken(accessTokenRequest);
        String idToken = token.getIdToken();

        Map<String, List<String>> expectedMap = new HashMap<String, List<String>>();
        expectedMap.put("scope", Collections.singletonList("openid"));
        expectedMap.put("grant_type", Collections.singletonList("client_credentials"));

        /*
        httpRequest = httpProvider.getRequest(
                clientAuthorizer, method, url, authorizationRequest.toFormParams());
                */
        Mockito.verify(mockHttpProvider, times(1))
                .getRequest(Mockito.any(HttpProvider.HttpRequestAuthorizer.class), Mockito.anyString(), Mockito.anyString(),
                        Mockito.eq(expectedMap));
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
        String expectedScope = "hrn:here:authorization::rlm0000:project/my-project-0000";
        String responseBody = getResponseBody(expectedAccessToken, expectedScope);
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

        Mockito.verify(mockHttpProvider, times(3))
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
        String expectedScope = "hrn:here:authorization::rlm0000:project/my-project-0000";
        String responseBody = getResponseBody(expectedAccessToken, expectedScope);
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
        String scope = accessTokenResponse.getScope();
        assertTrue("expected scope " + expectedScope + ", actual " + scope,
                expectedScope.equals(scope));

        Mockito.verify(mockHttpProvider, times(3))
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
            final String expectedScope = "hrn:here:authorization::rlm0000:project/my-project-0000";
            writeToFile(file, getResponseBody(expectedAccessToken, expectedScope));

            Mockito.doReturn("file://" + file.getAbsolutePath())
                    .when(mockClientAuthorizationRequestProvider).getTokenEndpointUrl();

            TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider,
                    mockClientAuthorizationRequestProvider);

            AccessTokenResponse accessTokenResponse = tokenEndpoint.requestToken(new IdentityTokenRequest());
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            String accessToken = accessTokenResponse.getAccessToken();
            assertTrue("expected access token " + expectedAccessToken + ", actual " + accessToken,
                    expectedAccessToken.equals(accessToken));
            String scope = accessTokenResponse.getScope();
            assertTrue("expected scope " + expectedScope + ", actual " + scope,
                    expectedScope.equals(scope));
        } finally {
            file.delete();
        }
    }


    @Test
    public void test_requestTokenNoScopeFromFile() throws IOException {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        ClientAuthorizationRequestProvider mockClientAuthorizationRequestProvider =
                Mockito.mock(ClientAuthorizationRequestProvider.class);

        File file = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        try {
            final String expectedAccessToken = "ey23.45";
            writeToFile(file, getResponseBody(expectedAccessToken, null));

            Mockito.doReturn("file://" + file.getAbsolutePath())
                    .when(mockClientAuthorizationRequestProvider).getTokenEndpointUrl();

            TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider,
                    mockClientAuthorizationRequestProvider);

            AccessTokenResponse accessTokenResponse = tokenEndpoint.requestToken(new IdentityTokenRequest());
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            String accessToken = accessTokenResponse.getAccessToken();
            assertTrue("expected access token " + expectedAccessToken + ", actual " + accessToken,
                    expectedAccessToken.equals(accessToken));
            String scope = accessTokenResponse.getScope();
            assertNull("expected scope to be NULL, actual " + scope, scope);
        } finally {
            file.delete();
        }
    }


    protected static String getResponseBody(String expectedAccessToken, String scope) {
        StringBuffer responseBody = new StringBuffer("{\"access_token\":\""+expectedAccessToken+"\",\"expires_in\":54321");
        if (null == scope) {
            responseBody.append("}");
        } else {
            responseBody.append(",\"scope\":\""+scope+"\"}");
        }
        return responseBody.toString();
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
                new OAuth1ClientCredentialsProvider(mySettableClock, url, accessKeyId, accessKeySecret, scope),
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

            @Override
            public Map<String, List<String>> getHeaders() {
                Map<String, List<String>> responseHeader = new HashMap<String, List<String>>();
                List<String> responseTypes = new ArrayList<String>();
                responseTypes.add(HttpConstants.CONTENT_TYPE_JSON);
                responseHeader.put(HttpConstants.CONTENT_TYPE, responseTypes);
                return responseHeader;
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

    @Test
    public void testGetFreshTokenVerifyUserSpecifiedExpiration() throws Exception {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        String body = "{\"access_token\":\"my-token\",\"expires_in\":50}";
        final HttpProvider.HttpResponse mockHttpResponse = new HttpProvider.HttpResponse() {

            @Override
            public int getStatusCode() {
                return 200;
            }

            @Override
            public long getContentLength() {
                return body.getBytes(StandardCharsets.UTF_8).length;
            }

            @Override
            public Map<String, List<String>> getHeaders() {
                Map<String, List<String>> headers = new HashMap<String, List<String>>();
                //headers.put(correlationIdKey, Collections.singletonList(correlationId));
                return headers;
            }

            @Override
            public InputStream getResponseBody() throws IOException {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                return new ByteArrayInputStream(bytes);
            }
        };
        Mockito.when(mockHttpProvider.execute(Mockito.any())).thenReturn(mockHttpResponse);

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                mockHttpProvider,
                new OAuth1ClientCredentialsProvider(new SettableSystemClock(),
                        url, accessKeyId, accessKeySecret, scope)
        );

        Long expiresIn = 50L;
        Fresh<AccessTokenResponse> freshToken = tokenEndpoint.requestAutoRefreshingToken(new ClientCredentialsGrantRequest().setExpiresIn(expiresIn));
        Long actualExpiresIn = freshToken.get().getExpiresIn();
        Long difference = expiresIn - actualExpiresIn;
        Long acceptableDifference = 2L;

        Assert.assertTrue("ExpiresIn not within acceptable difference", (acceptableDifference >= difference));
    }
    
    @Test(expected = NullPointerException.class)
    public void test_getTokenEndpoint_old_null_url() {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        ClientCredentialsProvider mockClientCredentialsProvider =
                Mockito.mock(ClientCredentialsProvider.class);
        Mockito.doReturn(null)
                .when(mockClientCredentialsProvider).getTokenEndpointUrl();
        Mockito.doReturn(HttpConstants.HttpMethods.POST)
                .when(mockClientCredentialsProvider).getHttpMethod();

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider, mockClientCredentialsProvider);
        assertTrue("tokenEndpoint was null", null != tokenEndpoint);

        AccessTokenRequest accessTokenRequest = new IdentityTokenRequest();
        // we will get an error as the apache http response is null.
        tokenEndpoint.requestToken(accessTokenRequest);
    }
    
    @Test(expected = NullPointerException.class)
    public void test_getTokenEndpoint_old_non_null_clock() {
        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        ClientCredentialsProvider mockClientCredentialsProvider =
                Mockito.mock(ClientCredentialsProvider.class);
        Mockito.doReturn(null)
                .when(mockClientCredentialsProvider).getTokenEndpointUrl();
        Mockito.doReturn(HttpConstants.HttpMethods.POST)
                .when(mockClientCredentialsProvider).getHttpMethod();
        Clock myClock = Clock.SYSTEM;
        Mockito.doReturn(myClock).when(mockClientCredentialsProvider).getClock();

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider, mockClientCredentialsProvider);
        assertTrue("tokenEndpoint was null", null != tokenEndpoint);

        AccessTokenRequest accessTokenRequest = new IdentityTokenRequest();
        // we will get an error as the apache http response is null.
        tokenEndpoint.requestToken(accessTokenRequest);
    }

    @Test
    public void test_requestHeader_with_additionalHeader_and_correlationId() throws Exception{
        String testKey = "testKey";
        String testValue = "testValue";
        String correlationIdKey = "X-Correlation-ID";
        final String correlationId = "abc123";

        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class
//                , Mockito
//                        .withSettings()
//                        .name("mockHttpProvider")
//                        .verboseLogging()
        );
        String body = "{\"access_token\":\"my-token\",\"expires_in\":50}";

        final HttpProvider.HttpResponse mockHttpResponse = new HttpProvider.HttpResponse() {
            @Override
            public int getStatusCode() {
                return 200;
            }

            @Override
            public long getContentLength() {
                return body.getBytes(StandardCharsets.UTF_8).length;
            }

            @Override
            public Map<String, List<String>> getHeaders() {
                Map<String, List<String>> headers = new HashMap<String, List<String>>();
                headers.put(correlationIdKey, Collections.singletonList(correlationId));
                return headers;
            }

            @Override
            public InputStream getResponseBody() throws IOException {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                return new ByteArrayInputStream(bytes);
            }
        };
        Mockito.when(mockHttpProvider.execute(Mockito.any())).thenReturn(mockHttpResponse);

        OAuth1Signer mockOauth1Signer = new OAuth1Signer(accessKeyId, accessKeySecret);

        ClientCredentialsProvider mockClientCredentialsProvider = Mockito.mock(ClientCredentialsProvider.class
//                , Mockito
//                        .withSettings()
//                        .name("mockClientCredentialsProvider")
//                        .verboseLogging()
        );
        Mockito.doReturn("https://www.example.com/oauth2/token")
                .when(mockClientCredentialsProvider).getTokenEndpointUrl();
        Mockito.doReturn(mockOauth1Signer)
                .when(mockClientCredentialsProvider).getClientAuthorizer();
        Mockito.doReturn(HttpConstants.HttpMethods.POST)
                .when(mockClientCredentialsProvider).getHttpMethod();

        HttpProvider.HttpRequest mockHttpRequest = Mockito.mock(HttpProvider.HttpRequest.class
//                , Mockito
//                        .withSettings()
//                        .name("mockHttpRequest")
//                        .verboseLogging()
        );
        Mockito.when(mockHttpProvider.getRequest(Mockito.any(HttpProvider.HttpRequestAuthorizer.class),
                Mockito.anyString(), Mockito.anyString(), Mockito.any(Map.class))).thenReturn(mockHttpRequest);

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider, mockClientCredentialsProvider);

        AccessTokenRequest accessTokenRequest = new ClientCredentialsGrantRequest();
        accessTokenRequest.setAdditionalHeaders(Collections.singletonMap(testKey, testValue));
        accessTokenRequest.setCorrelationId(correlationId);
        accessTokenRequest.setExpiresIn(1L);    // no need for a long lived token

        AccessTokenResponse accessTokenResponse = tokenEndpoint.requestToken(accessTokenRequest);

        assertTrue("accessTokenResponse was null", null != accessTokenResponse);
        String actualCorrelationId = accessTokenResponse.getCorrelationId();
        assertTrue("accessTokenResponse.getCorrelationId() was expected " + correlationId + ", actual " + actualCorrelationId,
        correlationId.equals(actualCorrelationId));

        // verify the expected values added to the request header
        Mockito.verify(mockHttpRequest, times(2)).addHeader(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockHttpRequest, times(1)).addHeader(testKey, testValue);
        Mockito.verify(mockHttpRequest, times(1)).addHeader(correlationIdKey, correlationId);
    }

    @Test
    public void test_requestHeader_with_correlationId() throws Exception{
        String correlationIdKey = "X-Correlation-ID";
        String correlationId = "fooBarBaz";

        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class
//                , Mockito
//                        .withSettings()
//                        .name("mockHttpProvider")
//                        .verboseLogging()
        );
        String body = "{\"access_token\":\"my-token\",\"expires_in\":50}";

        final HttpProvider.HttpResponse mockHttpResponse = new HttpProvider.HttpResponse() {
            @Override
            public int getStatusCode() {
                return 200;
            }

            @Override
            public long getContentLength() {
                return body.getBytes(StandardCharsets.UTF_8).length;
            }

            @Override
            public Map<String, List<String>> getHeaders() {
                Map<String, List<String>> headers = new HashMap<String, List<String>>();
                headers.put(correlationIdKey, Collections.singletonList(correlationId));
                return headers;
            }

            @Override
            public InputStream getResponseBody() throws IOException {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                return new ByteArrayInputStream(bytes);
            }
        };
        Mockito.when(mockHttpProvider.execute(Mockito.any())).thenReturn(mockHttpResponse);

        OAuth1Signer mockOauth1Signer = new OAuth1Signer(accessKeyId, accessKeySecret);

        ClientCredentialsProvider mockClientCredentialsProvider = Mockito.mock(ClientCredentialsProvider.class
//                , Mockito
//                        .withSettings()
//                        .name("mockClientCredentialsProvider")
//                        .verboseLogging()
        );
        Mockito.doReturn("https://www.example.com/oauth2/token")
                .when(mockClientCredentialsProvider).getTokenEndpointUrl();
        Mockito.doReturn(mockOauth1Signer)
                .when(mockClientCredentialsProvider).getClientAuthorizer();
        Mockito.doReturn(HttpConstants.HttpMethods.POST)
                .when(mockClientCredentialsProvider).getHttpMethod();

        HttpProvider.HttpRequest mockHttpRequest = Mockito.mock(HttpProvider.HttpRequest.class
//                , Mockito
//                        .withSettings()
//                        .name("mockHttpRequest")
//                        .verboseLogging()
        );
        Mockito.when(mockHttpProvider.getRequest(Mockito.any(HttpProvider.HttpRequestAuthorizer.class),
                Mockito.anyString(), Mockito.anyString(), Mockito.any(Map.class))).thenReturn(mockHttpRequest);

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider, mockClientCredentialsProvider);

        AccessTokenRequest accessTokenRequest = new ClientCredentialsGrantRequest();
        accessTokenRequest.setCorrelationId(correlationId);
        accessTokenRequest.setExpiresIn(1L);    // no need for a long lived token

        AccessTokenResponse response = tokenEndpoint.requestToken(accessTokenRequest);
        assertTrue("response was null", null != response);
        String actualCorrelationId = response.getCorrelationId();
        assertTrue("correlationId was expected " + correlationId + ", actual " + actualCorrelationId,
        correlationId.equals(actualCorrelationId));

        // verify the expected value added to the request header
        Mockito.verify(mockHttpRequest, times(1)).addHeader(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockHttpRequest, times(1)).addHeader(correlationIdKey, correlationId);
    }

    @Test
    public void test_requestResponse_with_correlationId() {
        String expectedCorrelationId = "abc123";
        HttpProvider httpProvider = getHttpProvider();
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                httpProvider,
                new OAuth1ClientCredentialsProvider(new SettableSystemClock(),
                        url, accessKeyId, accessKeySecret));

        AccessTokenRequest accessTokenRequest = new ClientCredentialsGrantRequest();
        accessTokenRequest.setCorrelationId(expectedCorrelationId);
        AccessTokenResponse token = tokenEndpoint.requestToken(accessTokenRequest);

        assertEquals(expectedCorrelationId, token.getCorrelationId());
    }
}
