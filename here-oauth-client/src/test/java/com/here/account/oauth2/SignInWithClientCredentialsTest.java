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

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;

public class SignInWithClientCredentialsTest extends AbstractCredentialTezt {

    HttpProvider httpProvider;
    TokenEndpoint signIn;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        httpProvider = ApacheHttpClientProvider.builder()
        .setConnectionTimeoutInMs(HttpConstants.DEFAULT_CONNECTION_TIMEOUT_IN_MS)
        .setRequestTimeoutInMs(HttpConstants.DEFAULT_REQUEST_TIMEOUT_IN_MS)
        .build();
        
        this.signIn = HereAccount.getTokenEndpoint(
                httpProvider,
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret)
        );
    }
    
    @After
    public void tearDown() throws IOException {
        if (null != httpProvider) {
            httpProvider.close();
        }
    }

    @Test
    public void test_signIn() throws Exception {
        String hereAccessToken = signIn.requestToken(new ClientCredentialsGrantRequest()).getAccessToken();
        assertTrue("hereAccessToken was null or blank", null != hereAccessToken && hereAccessToken.length() > 0);
    }
    
    @Test
    public void test_signIn_fatFinger() throws Exception {
        this.signIn = HereAccount.getTokenEndpoint(
                httpProvider,
                new OAuth1ClientCredentialsProvider(url, accessKeyId, "fat" + accessKeySecret)
        );

        try{
            signIn.requestToken(new ClientCredentialsGrantRequest()).getAccessToken();
        } catch (AccessTokenException e) {
            ErrorResponse errorResponse = e.getErrorResponse();
            assertTrue("errorResponse was null", null != errorResponse);
            Integer errorCode = errorResponse.getErrorCode();
            Integer expectedErrorCode = 401300;
            assertTrue("errorCode was expected "+expectedErrorCode+", actual "+errorCode, expectedErrorCode.equals(errorCode));
        }

    }
    
    @Test
    public void test_signIn_expiresIn() throws Exception {
        AccessTokenResponse accessTokenResponse = signIn.requestToken(new ClientCredentialsGrantRequest());
        String hereAccessToken = accessTokenResponse.getAccessToken();
        assertTrue("hereAccessToken was null or blank", 
                null != hereAccessToken && hereAccessToken.length() > 0);
        
        AccessTokenResponse accessTokenResponse15 = signIn.requestToken(new ClientCredentialsGrantRequest().setExpiresIn(15L));
        String hereAccessToken15 = accessTokenResponse15.getAccessToken();
        assertTrue("hereAccessToken15 was null or blank", 
                null != hereAccessToken15 && hereAccessToken15.length() > 0);
    
        long expiresIn = accessTokenResponse.getExpiresIn();
        long expiresIn15 = accessTokenResponse15.getExpiresIn();
        assertTrue("expiresIn15 "+expiresIn15+" !< expiresIn "+expiresIn, 
                expiresIn15 < expiresIn);
        
        // Verification adds some tolerance to the exact number of seconds.
        // The server should not think it's valid longer than 15 seconds.
        // The client should have been successful telling the server this, 
        // and the client should be able to deserialize the response properly 
        // for this constraint to hold.
        assertTrue("expiresIn15 "+expiresIn15+" not in range",
                1 <= expiresIn15 && expiresIn15 <= 15);
    }
    
}
