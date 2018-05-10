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

import com.here.account.util.SettableSystemClock;
import org.junit.Assert;
import org.junit.Test;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpProvider;

public abstract class AbstractHereAccountProviderTezt extends AbstractCredentialTezt {

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
        HttpProvider httpProvider = getHttpProvider();
        
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                httpProvider, 
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
                    +expectedError+", actual "+error+", errorResponse="+errorResponse,
                    expectedError.equals(error));
        }
    }
    
    @Test
    public void testGetToken() throws Exception {
        HttpProvider httpProvider = getHttpProvider();
        
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                httpProvider, 
                new OAuth1ClientCredentialsProvider(new SettableSystemClock(),
                        url, accessKeyId, accessKeySecret));
        
        AccessTokenResponse accessTokenResponse = tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
        assertTrue("accessTokenResponse was null", null != accessTokenResponse);
        String accessToken = accessTokenResponse.getAccessToken();
        assertTrue("accessToken was null or zero-length", null != accessToken && accessToken.length() > 0);
    }

}
