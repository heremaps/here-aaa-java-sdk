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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpResponse;
import com.here.account.http.java.JavaHttpProvider;

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
    @SuppressWarnings("unused") // code snippet from Javadocs verbatim; intentionally has unused variable
    public void test_simpleUseCase_javadocs() throws IOException, AccessTokenException, RequestExecutionException, ResponseParsingException {
        // use your provided credentials.properties
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                getHttpProvider(), 
                new OAuth1ClientCredentialsProvider.FromFile(new File("credentials.properties")));
        
        String hereAccessToken = tokenEndpoint.requestToken(
                new ClientCredentialsGrantRequest()).getAccessToken();
        // use hereAccessToken on requests until expires...
    }

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
