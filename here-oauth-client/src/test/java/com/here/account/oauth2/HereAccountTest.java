/*
 * Copyright 2016 HERE Global B.V.
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
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.http.HttpProvider.HttpResponse;
import org.junit.Test;

import com.here.account.http.apache.ApacheHttpClientProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.Assert;

public class HereAccountTest extends AbstractCredentialTezt {

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
    
    @Test
    public void testGetTokenInvalidCredentials() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, "invalidSecret"));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected AccessTokenException");
        } catch (AccessTokenException ate) {
            
        }
    }
    
    @Test
    public void testGetTokenInvalidResponse() throws Exception {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                new MockHttpProvider(
                        ApacheHttpClientProvider.builder().build(),
                        new ByteArrayInputStream("bogus".getBytes("UTF-8"))),
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
                new MockHttpProvider(
                        ApacheHttpClientProvider.builder().build(),
                        new ByteArrayInputStream("bogus".getBytes("UTF-8"))),
                new OAuth1ClientCredentialsProvider(url, accessKeyId, "invalidSecret"));
        
        try {
            tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
            Assert.fail("Expected ResponseParsingException");
        } catch (ResponseParsingException rpe) {
            
        }
    }
    
    
    /**
     * Wrap an HttpProvider into a provider that will always
     * intercept the response and return the provided response body.
     */
    private static class MockHttpProvider implements HttpProvider {
        private final HttpProvider wrapped;
        private final InputStream mockResponse;
        private MockHttpProvider(HttpProvider wrapped,
                                  InputStream mockResponse) {
            this.wrapped = wrapped;
            this.mockResponse = mockResponse;
        }

        @Override
        public HttpResponse execute(HttpRequest httpRequest) throws HttpException, IOException {
            HttpResponse response = wrapped.execute(httpRequest);
            return new HttpResponse() {
                @Override
                public int getStatusCode() {
                    return response.getStatusCode();
                }

                @Override
                public long getContentLength() {
                    return response.getContentLength();
                }

                @Override
                public InputStream getResponseBody() throws IOException {
                    return mockResponse;
                }
            };
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
        }

        @Override
        public HttpRequest getRequest(HttpRequestAuthorizer httpSigner, String method, String url, Map<String, List<String>> formParams) {
            return wrapped.getRequest(httpSigner, method, url, formParams);
        }

        @Override
        public HttpRequest getRequest(HttpRequestAuthorizer httpRequestAuthorizer, String method, String url, String requestBodyJson) {
            return wrapped.getRequest(httpRequestAuthorizer, method, url, requestBodyJson);
        }
        
        
        
    }
}
