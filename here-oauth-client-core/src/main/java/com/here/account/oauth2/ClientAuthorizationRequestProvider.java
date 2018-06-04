/*
 * Copyright (c) 2018 HERE Europe B.V.
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

import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider;
import com.here.account.util.Clock;

/**
 * A {@code ClientAuthorizationRequestProvider} identifies a token endpoint,
 * provides a mechanism to use credentials to authorize access token requests,
 * and provides access token request objects.
 */
public interface ClientAuthorizationRequestProvider {

    /**
     * Gets the url of the token endpoint for this OAuth 2.0 Provider.
     * See also <a href="https://tools.ietf.org/html/rfc6749#section-3.2">The
     * OAuth 2.0 Authorization Framework: Token Endpoint</a>.
     * 
     * @return the url of the token endpoint
     */
    String getTokenEndpointUrl();
    
    /**
     * Gets the {@code HttpRequestAuthorizer} that the client will use
     * to authorize access token requests.
     * 
     * @return the client authorizer
     */
    HttpProvider.HttpRequestAuthorizer getClientAuthorizer();
    
    /**
     * Gets a new AccessTokenRequest to authorize this client to obtain 
     * an Access Token.
     * 
     * @return the new Access Token Request
     */
    AccessTokenRequest getNewAccessTokenRequest();
    
    /**
     * Get the HTTP Method used to obtain authorization.
     * 
     * @return the HTTP Method
     */
    HttpMethods getHttpMethod();

    /**
     * Get the Clock implementation in use.
     *
     * @return the Clock in use.
     */
    Clock getClock();
    
}
