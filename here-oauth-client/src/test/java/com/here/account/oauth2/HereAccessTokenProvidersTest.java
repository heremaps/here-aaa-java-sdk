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

import java.io.IOException;

import org.junit.Test;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.ClientCredentialsGrantRequest;
import com.here.account.util.RefreshableResponseProvider;

public class HereAccessTokenProvidersTest extends AbstractCredentialTezt {

    @Test
    public void test_getSignIn_javadocs() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        // set up urlStart, clientId, and clientSecret.
        AuthorizationObtainer authorizationObtainer = HereAccessTokenProviders.getSignIn(
             ApacheHttpClientProvider.builder().build(), 
             urlStart, clientId, clientSecret);
        String hereAccessToken = authorizationObtainer.postToken(
             new ClientCredentialsGrantRequest()).getAccessToken();
        // use hereAccessToken on requests until expires...
    }
    
    @Test
    public void test_getRefreshableClientAuthorizationProvider_javadocs() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        // set up urlStart, clientId, and clientSecret.
        // call this once and keep a reference to refreshableResponseProvider, such as in your beans
        RefreshableResponseProvider<AccessTokenResponse> refreshableResponseProvider = 
            HereAccessTokenProviders.getRefreshableClientAuthorizationProvider(
                 ApacheHttpClientProvider.builder().build(), 
                 urlStart, clientId, clientSecret);
        // using your reference to refreshableResponse, for each request, just ask for a new hereAccessToken
        // the same hereAccessToken is returned for most of the valid time; but as it nears 
        // expiry the returned value will change.
        String hereAccessToken = refreshableResponseProvider.getUnexpiredResponse().getAccessToken();
        // use hereAccessToken on your request...
    }
    
}
