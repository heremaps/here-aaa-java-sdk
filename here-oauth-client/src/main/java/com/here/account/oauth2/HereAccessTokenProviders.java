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
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.ClientCredentialsGrantRequest;
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
        // set up urlStart, clientId, and clientSecret.
        AuthorizationObtainer authorizationObtainer = HereAccessTokenProviders
             .getAuthorizationObtainer(
                     ApacheHttpClientProvider.builder().build(), 
                     urlStart, clientId, clientSecret);
        String hereAccessToken = authorizationObtainer.postToken(
             new ClientCredentialsGrantRequest()).getAccessToken();
        // use hereAccessToken on requests until expires...
       }
     * </pre>
     * 
     * @param httpProvider the HTTP-layer provider implementation
     * @param urlStart the protocol, host, and port portion of the HERE authorization server endpoint you want to call.
     * @param clientId see also <a href="https://tools.ietf.org/html/rfc6749#section-2.3.1">client_id</a>; 
     *     as recommended by the RFC, we don't provide this in the body, but make it part of the request signature.
     * @param clientSecret see also <a href="https://tools.ietf.org/html/rfc6749#section-2.3.1">client_secret</a>; 
     *     as recommended by the RFC, we don't provide this in the body, but make it part of the request signature.
     * @return the ability to SignIn.
     */
    public static AuthorizationObtainer getAuthorizationObtainer(
            HttpProvider httpProvider,
            String urlStart, String clientId, String clientSecret) {
        return
                new AuthorizationObtainer( httpProvider,  urlStart,  clientId,  clientSecret 
                        );
    }
    
    /**
     * Get a RefreshableResponseProvider where when you invoke 
     * {@link RefreshableResponseProvider#getUnexpiredResponse()}, 
     * you will always get a current HERE Access Token, 
     * for the grant_type=client_credentials use case, for 
     * confidential clients.
     * 
     * <p>
     * Example code:
     * <pre>
     * {@code
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
     * </pre>
     *
     * @param httpProvider the HTTP-layer provider implementation
     * @param urlStart the protocol, host, and port portion of the HERE authorization server endpoint you want to call.
     * @param clientId see also <a href="https://tools.ietf.org/html/rfc6749#section-2.3.1">client_id</a>; 
     *     as recommended by the RFC, we don't provide this in the body, but make it part of the request signature.
     * @param clientSecret see also <a href="https://tools.ietf.org/html/rfc6749#section-2.3.1">client_secret</a>; 
     *     as recommended by the RFC, we don't provide this in the body, but make it part of the request signature.
     * @return the refreshable response provider presenting an always "fresh" client_credentials-based HERE Access Token.
     * @throws IOException if I/O trouble processing the request
     * @throws AuthenticationHttpException if you had trouble authenticating your request to the authorization server, 
     *      or the authorization server rejected your request
     * @throws HttpException if an exception from the provider
     */
    public static RefreshableResponseProvider<AccessTokenResponse> getRefreshableClientAuthorizationProvider(
            HttpProvider httpProvider,
            String urlStart, String clientId, String clientSecret) throws IOException, AuthenticationHttpException, HttpException {
        AuthorizationObtainer authorizationObtainer = 
                getAuthorizationObtainer( httpProvider,  urlStart,  clientId,  clientSecret 
                        );
        Long optionalRefreshIntervalMillis = null;
        return new RefreshableResponseProvider<AccessTokenResponse>(
                optionalRefreshIntervalMillis,
                authorizationObtainer.postToken(new ClientCredentialsGrantRequest()),
                new ClientCredentialsRefresher(authorizationObtainer));
    }
    
}
