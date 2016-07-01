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

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.ClientCredentialsGrantRequest;
import com.here.account.oauth2.bo.PasswordGrantRequest;
import com.here.account.util.RefreshableResponseProvider;

/**
 * Get a HERE Access Token to use on requests to HERE Service REST APIs according to 
 * <a href="https://tools.ietf.org/html/rfc6750">The OAuth 2.0 Authorization Framework: Bearer Token Usage</a>.
 * See also the OAuth2.0 
 * <a href="https://tools.ietf.org/html/rfc6749#section-1.4">Access Token</a> spec.
 * 
 * @author kmccrack
 *
 */
public class HereAccessTokenProviders {

    /**
     * Get the ability to run various Obtaining Authorization API calls to the 
     * HERE Account Authorization Server.
     * See OAuth2.0 
     * <a href="https://tools.ietf.org/html/rfc6749#section-4">Obtaining Authorization</a>.
     * 
     * <p>
     * Example code:
     * <pre>
     * {@code
     * // set up urlStart, clientId, and clientSecret.
     * SignIn signIn = HereAccessTokenProviders.getSignIn(
     *      ApacheHttpClientProvider.builder().build(), 
     *      urlStart, clientId, clientSecret);
     * String hereAccessToken = signIn.signIn(
     *      new ClientCredentialsGrantRequest()).getAccessToken();
     * // use hereAccessToken on requests until expires...
     * }
     * </pre>
     * 
     * @param httpProvider
     * @param urlStart
     * @param clientId
     * @param clientSecret
     * @return
     */
    public static SignIn getSignIn(
            HttpProvider httpProvider,
            String urlStart, String clientId, String clientSecret) {
        return
                new SignIn( httpProvider,  urlStart,  clientId,  clientSecret 
                        );
    }
    
    /**
     * Get the an object where when you invoke {@link RefreshableResponseProvider#getClass()}, 
     * you will always get a current HERE Access Token, 
     * for the client_credentials grant use case.
     * 
     * <p>
     * Example code:
     * <pre>
     * {@code
     * // set up urlStart, clientId, and clientSecret.
     * // call this once and keep a reference to refreshableResponseProvider, such as in your beans
     * RefreshableResponseProvider<AccessTokenResponse> refreshableResponseProvider = 
     *     HereAccessTokenProviders.getRefreshableClientCredentialsProvider(
     *          ApacheHttpClientProvider.builder().build(), 
     *          urlStart, clientId, clientSecret,
     *          null);
     * // using your reference to refreshableResponse, for each request, just ask for a new hereAccessToken
     * // the same hereAccessToken is returned for most of the valid time; but as it nears 
     * // expiry the returned value will change.
     * String hereAccessToken = refreshableResponseProvider.getUnexpiredResponse().getAccessToken();
     * // use hereAccessToken on your request...
     *
     * @param httpProvider
     * @param urlStart
     * @param clientId
     * @param clientSecret
     * @param optionalRefreshInterval
     * @return
     * @throws AuthenticationRuntimeException
     * @throws IOException
     * @throws AuthenticationHttpException
     * @throws HttpException
     */
    public static RefreshableResponseProvider<AccessTokenResponse> getRefreshableClientCredentialsProvider(
            HttpProvider httpProvider,
            String urlStart, String clientId, String clientSecret, 
            Long optionalRefreshInterval) throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        SignIn signIn = 
                getSignIn( httpProvider,  urlStart,  clientId,  clientSecret 
                        );
        return new RefreshableResponseProvider<AccessTokenResponse>(
                optionalRefreshInterval,
                signIn.signIn(new ClientCredentialsGrantRequest()),
                new ClientCredentialsRefresher(signIn));
    }
    
    public static RefreshableResponseProvider<AccessTokenResponse> getRefreshableUserCredentialsProvider(
            HttpProvider httpProvider,
            String urlStart, String clientId, String clientSecret, 
            String email, String password,
            Long optionalRefreshInterval
            ) throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        SignIn signIn = 
                getSignIn( httpProvider,  urlStart,  clientId,  clientSecret 
                        );
        PasswordGrantRequest passwordGrantRequest = new PasswordGrantRequest().setEmail(email).setPassword(password);
        AccessTokenResponse accessTokenResponse = 
                signIn.signIn(passwordGrantRequest);
        Long refreshIntervalMillis = null;
        RefreshableResponseProvider<AccessTokenResponse> refreshableResponseProvider 
        = new RefreshableResponseProvider<AccessTokenResponse>(
                refreshIntervalMillis,
                accessTokenResponse,
                new UserCredentialsRefresher(signIn)
                );
        return refreshableResponseProvider;
    }

}
