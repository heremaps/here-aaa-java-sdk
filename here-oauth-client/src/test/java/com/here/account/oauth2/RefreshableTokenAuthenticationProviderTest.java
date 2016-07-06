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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.util.RefreshableResponseProvider;

public class RefreshableTokenAuthenticationProviderTest extends AbstractCredentialTezt {

    HttpProvider httpProvider;
    RefreshableResponseProvider<AccessTokenResponse> provider;
    
    @Before
    public void setUp() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        super.setUp();
        
        httpProvider = ApacheHttpClientProvider.builder()
        .setConnectionTimeoutInMs(HttpConstants.DEFAULT_CONNECTION_TIMEOUT_IN_MS)
        .setRequestTimeoutInMs(HttpConstants.DEFAULT_REQUEST_TIMEOUT_IN_MS)
        .build();

        this.provider = (RefreshableResponseProvider<AccessTokenResponse>) HereAccessTokenProviders
                .getRefreshableClientAuthorizationProvider(
                        httpProvider,
                        urlStart, clientId, clientSecret, 
                100L);
    }
    
    protected static void verifyLoopChanges(RefreshableResponseProvider<AccessTokenResponse> signedIn) throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        String hereAccessToken = signedIn.getUnexpiredResponse().getAccessToken();
        assertTrue("hereAccessToken was null or blank", null != hereAccessToken && hereAccessToken.length() > 0);

        Set<String> allAccessTokens = new HashSet<String>();
        allAccessTokens.add(hereAccessToken);
        for (int i = 0; i < 3; i++) {
            String newAccessToken = verifyChanges(signedIn, hereAccessToken);
            assertTrue("newAccessToken wasn't new", allAccessTokens.add(newAccessToken));
            hereAccessToken = newAccessToken;
        }
        
        // test a blocking call
        try {
            Thread.sleep(2001L);
        } catch (InterruptedException e) {
            assertTrue("interrupted: "+e, false);
        }
        String newAccessToken = verifyChanges(signedIn, hereAccessToken);
        assertTrue("newAccessToken wasn't new", allAccessTokens.add(newAccessToken));

    }

    protected static String verifyChanges(
            RefreshableResponseProvider<AccessTokenResponse> signedIn, String hereAccessToken) throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        long expires = System.currentTimeMillis() + 10*1000L;
        String newAccessToken = hereAccessToken;
        int countSame = 0;
        while(System.currentTimeMillis() < expires && hereAccessToken.equals(newAccessToken)) {
            newAccessToken = signedIn.getUnexpiredResponse().getAccessToken();
            if (hereAccessToken.equals(newAccessToken)) {
                countSame++;
            }
        }
        System.out.println(countSame + " calls using same Here Access Token");
        assertTrue("newAccessToken hadn't changed", 
                newAccessToken != null && newAccessToken.length() > 0 && !hereAccessToken.equals(newAccessToken));
        return newAccessToken;
    }



    
    @Test
    public void test_10tokens() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        verifyLoopChanges(provider);
    }
    
    @After
    public void tearDown() throws IOException {
        try {
            if (null != httpProvider) {
                httpProvider.close();
            } 
        } finally {
            if (null != provider) {
                this.provider.shutdown();
            }
        }
    }
}
