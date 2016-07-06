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
import java.io.InputStream;

import com.here.account.auth.OAuth1Signer;
import com.here.account.bo.AuthenticationHttpException;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.AuthorizationRequest;
import com.here.account.oauth2.bo.ErrorResponse;
import com.here.account.util.JsonSerializer;

public class SignIn {

    public static final String POST_TOKEN_PATH = "/oauth2/token";
    public static final String HTTP_METHOD_POST = "POST";

    private HttpProvider httpProvider;
    private String urlStart;
    private OAuth1Signer oauth1Signer;

    /**
     * Construct a new ability to SignIn to the HERE authorization server.
     * 
     * @param httpProvider the HTTP-layer provider implementation
     * @param urlStart the protocol, host, and port portion of the HERE authorization server endpoint you want to call.
     * @param clientId see also <a href="https://tools.ietf.org/html/rfc6749#section-2.3.1">client_id</a>; 
     *     as recommended by the RFC, we don't provide this in the body, but make it part of the request signature.
     * @param clientSecret see also <a href="https://tools.ietf.org/html/rfc6749#section-2.3.1">client_secret</a>; 
     *     as recommended by the RFC, we don't provide this in the body, but make it part of the request signature.
     */
    SignIn(HttpProvider httpProvider, String urlStart, String clientId, String clientSecret 
            ) {
        this.httpProvider = httpProvider;
        this.urlStart = urlStart;
        this.oauth1Signer = new OAuth1Signer(clientId, clientSecret);
    }

    /**
     * Get a HERE Access Token, for use with HERE Services.
     * Returns just the token, to be used as an Authorization: Bearer token value.
     * See <a href="https://tools.ietf.org/html/rfc6749#section-7.1">OAuth2.0</a>, 
     * and <a href="https://tools.ietf.org/html/rfc6750">OAuth2.0 Bearer Token Usage</a> 
     * for details.
     *
     * @param authorizationRequest the request for authorization
     * @return the Access Token that can be used as Bearer token for HERE Service requests
     * @throws IOException if I/O trouble processing the request
     * @throws AuthenticationHttpException if you had trouble authenticating your request to the authorization server, 
     *      or the authorization server rejected your request
     * @throws HttpException if an exception from the provider
     */
    public AccessTokenResponse postToken(AuthorizationRequest authorizationRequest) 
            throws IOException, AuthenticationHttpException, HttpException {
        String method = HTTP_METHOD_POST;
        
        String url = urlStart + POST_TOKEN_PATH;
        
        // OAuth2.0 uses application/x-www-form-urlencoded
        HttpRequest apacheRequest = httpProvider.getRequest(oauth1Signer, method, url, 
                authorizationRequest.toFormParams());

        // blocking
        HttpProvider.HttpResponse apacheResponse = httpProvider.execute(apacheRequest);
        
        int statusCode = apacheResponse.getStatusCode();
        InputStream jsonInputStream = null;
        try {
            jsonInputStream = apacheResponse.getResponseBody();
            if (200 == statusCode) {
                return JsonSerializer.toPojo(
                        LowerUpperCamelCaseToUnderscoreConverter.convertRootKeysToUnderscores(jsonInputStream)
                        , AccessTokenResponse.class);
            } else {
                ErrorResponse errorResponse = JsonSerializer.toPojo(jsonInputStream, ErrorResponse.class);
                throw new AuthenticationHttpException(statusCode, errorResponse);
            }
        } finally {
            if (null != jsonInputStream) {
                jsonInputStream.close();
            }
        }
    }

}
