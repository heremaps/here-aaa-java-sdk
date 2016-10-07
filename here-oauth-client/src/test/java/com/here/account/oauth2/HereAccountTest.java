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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpResponse;
import com.here.account.http.apache.ApacheHttpClientProvider;

public class HereAccountTest extends AbstractCredentialTezt {

    /**
     * We expect FileNotFoundException because we expect the current working directory 
     * not to contain credentials.properties.
     *
     * @throws IOException
     * @throws AccessTokenException
     * @throws RequestExecutionException
     * @throws ResponseParsingException
     */
    @Test(expected=FileNotFoundException.class) 
    public void test_simpleUseCase_javadocs() throws IOException, AccessTokenException, RequestExecutionException, ResponseParsingException {
        // use your provided credentials.properties
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider.FromFile(new File("credentials.properties")));
        
        String hereAccessToken = tokenEndpoint.requestToken(
                new ClientCredentialsGrantRequest()).getAccessToken();
        // use hereAccessToken on requests until expires...
    }

    @Test
    public void test_getSignIn_javadocs() throws AccessTokenException, RequestExecutionException, ResponseParsingException {
        // set up url, accessKeyId, and accessKeySecret.
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        
        String hereAccessToken = tokenEndpoint.requestToken(
                new ClientCredentialsGrantRequest()).getAccessToken();
        // use hereAccessToken on requests until expires...
    }
    
    @Test
    public void test_getRefreshableClientAuthorizationProvider_javadocs() throws AccessTokenException, RequestExecutionException, ResponseParsingException {
        // set up url, accessKeyId, and accessKeySecret.
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        // call this once and keep a reference to freshToken, such as in your beans
        Fresh<AccessTokenResponse> freshToken = tokenEndpoint.requestAutoRefreshingToken(
                new ClientCredentialsGrantRequest());
        
        // using your reference to freshToken, for each request, just ask for the token
        // the same hereAccessToken is returned for most of the valid time; but as it nears 
        // expiry the returned value will change.
        String hereAccessToken = freshToken.get().getAccessToken();
        // use hereAccessToken on your request...
    }
    
    /**
     * We expect FileNotFoundException because we expect the current working directory 
     * not to contain credentials.properties.
     * Clients are free to put their "credentials.properties" File anywhere on their filesystem, 
     * for positive outcomes.
     * This test verifies the Javadoc sample compiles; 
     * it would not throw any Exception if the File existed.
     * 
     * @throws IOException
     */
    @Test(expected=FileNotFoundException.class) 
    public void test_file_javadocs() throws IOException {
        // setup url, accessKeyId, and accessKeySecret as properties in credentials.properties
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider.FromFile(new File("credentials.properties")));
        // choose 
        //   tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
        // or 
        //   tokenEndpoint.requestAutoRefreshingToken(new ClientCredentialsGrantRequest());
    }
    

    
    @Test(expected=NullPointerException.class)
    public void testGetTokenNullUrl() throws Exception {
        HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(null, accessKeyId, accessKeySecret));
    }
    
    @Test(expected=NullPointerException.class)
    public void testGetTokenNullAccessKeyId() throws Exception {
        HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(url, null, accessKeySecret));
    }
    
    @Test(expected=NullPointerException.class)
    public void testGetTokenNullAccessKeySecret() throws Exception {
        HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, null));
    }
    
    @Test
    public void testGetTokenInvalidUrl() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider("bogus", accessKeyId, accessKeySecret));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected RequestExecutionException");
        } catch (RequestExecutionException ree) {
            
        }
    }
    
    /**
     * Confirms InvalidCredentials => AccessTokenException whose 
     * ErrorResponse object has error="invalid_client", so clients 
     * could potentially write code against the RFC6749 using these 
     * business objects.
     * 
     * @throws Exception if an unexpected Exception is thrown by the test.
     */
    @Test
    public void testGetToken_InvalidCredentials() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, "invalidSecret"));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected AccessTokenException");
        } catch (AccessTokenException ate) {
            ErrorResponse errorResponse = ate.getErrorResponse();
            assertTrue("errorResponse was null", null != errorResponse);
            String error = errorResponse.getError();
            final String expectedError = "invalid_client";
            assertTrue("\"error\" in JSON error response body was expected "
                    +expectedError+", actual "+error, 
                    expectedError.equals(error));
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
    public void testGetToken_MissingRequiredParameter() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
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
        
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                mockHttpProvider(dummyResponse(200, 
                                               validToken1.getBytes().length, 
                                               new ByteArrayInputStream(validToken1.getBytes("UTF-8"))),
                                 dummyResponse(200,
                                               validToken2.getBytes().length,
                                               new ByteArrayInputStream(validToken2.getBytes("UTF-8")))),
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        
        Fresh<AccessTokenResponse> freshToken = tokenEndpoint.
                requestAutoRefreshingToken(new ClientCredentialsGrantRequest());
        // verify validToken1
        Assert.assertEquals("12345", freshToken.get().getAccessToken());
        Assert.assertEquals("12345", freshToken.get().getAccessToken());
        // wait for refresh
        Thread.sleep(31000);
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
