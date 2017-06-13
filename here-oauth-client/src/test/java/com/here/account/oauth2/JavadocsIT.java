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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;

public class JavadocsIT extends AbstractCredentialTezt {
    
    /**
     * We expect FileNotFoundException because we expect the current working directory 
     * not to contain credentials.properties.
     *
     * @throws IOException
     * @throws AccessTokenException
     * @throws RequestExecutionException
     * @throws ResponseParsingException
     */
    // To use your provided credentials.properties to get a one-time use token:
    @Test(expected=FileNotFoundException.class) 
    @SuppressWarnings("unused") // code snippet from Javadocs verbatim; intentionally has unused variable
    public void test_simpleUseCase_javadocs() throws IOException, AccessTokenException, RequestExecutionException, ResponseParsingException {
        // use your provided credentials.properties
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider.FromFile(new File("credentials.properties")));
        
        String hereAccessToken = tokenEndpoint.requestToken(
                new ClientCredentialsGrantRequest()).getAccessToken();
        // use hereAccessToken on requests until expires...
    }

    // Get a one time use HERE Access Token:
    @Test
    @SuppressWarnings("unused") // code snippet from Javadocs verbatim; intentionally has unused variable
    public void test_getSingleAccessToken_javadocs() throws AccessTokenException, RequestExecutionException, ResponseParsingException {
        // set up url, accessKeyId, and accessKeySecret.
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        
        String hereAccessToken = tokenEndpoint.requestToken(
                new ClientCredentialsGrantRequest()).getAccessToken();
        // use hereAccessToken on requests until expires...
    }
    
    // Get an auto refreshing HERE Access Token:
    @Test
    @SuppressWarnings("unused") // code snippet from Javadocs verbatim; intentionally has unused variable
    public void test_getRefreshableAccessTokenProvider_javadocs() throws AccessTokenException, RequestExecutionException, ResponseParsingException {
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

}
