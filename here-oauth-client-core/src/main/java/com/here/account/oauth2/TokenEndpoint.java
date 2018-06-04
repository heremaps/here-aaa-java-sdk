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

import java.util.function.Supplier;

/**
 * A {@code TokenEndpoint} directly corresponds to the token endpoint as specified in
 * the OAuth2.0 Specification.  See
 * <a href="https://tools.ietf.org/html/rfc6749#section-3.2">OAuth2.0 Token Endpoint</a>.
 */
public interface TokenEndpoint {
    
    /**
     * POST to the token endpoint to get a HERE Access Token, for use with HERE Services.
     * Returns just the token, to be used as an Authorization: Bearer token value.
     * See <a href="https://tools.ietf.org/html/rfc6749#section-7.1">OAuth2.0</a>, 
     * and <a href="https://tools.ietf.org/html/rfc6750">OAuth2.0 Bearer Token Usage</a> 
     * for details.
     *
     * @param request the token request
     * @return the Access Token that can be used as Bearer token for HERE Service requests
     * @throws AccessTokenException if you had trouble authenticating your request to the authorization server, 
     *      or the authorization server rejected your request
     * @throws RequestExecutionException if trouble processing the request
     * @throws ResponseParsingException if trouble parsing the response
     */
    AccessTokenResponse requestToken(AccessTokenRequest request) 
            throws AccessTokenException, RequestExecutionException, ResponseParsingException;
    
    /**
     * POST to the token endpoint to get an always fresh HERE Access Token, for use with HERE Services.
     * The returned token is wrapped in a {@link Fresh}, and is periodically and automatically
     * refreshed before it expires.  The token can be used as an Authorization: Bearer token value.
     * See <a href="https://tools.ietf.org/html/rfc6749#section-7.1">OAuth2.0</a>, 
     * and <a href="https://tools.ietf.org/html/rfc6750">OAuth2.0 Bearer Token Usage</a> 
     * for details.
     *
     * @param request the token request
     * @return a {@link Fresh} wrapped Access Token that can be used as Bearer token for HERE Service requests
     *         the returned {@link Fresh} will always give an unexpired access token on a call to get()
     * @throws AccessTokenException if you had trouble authenticating your request to the authorization server, 
     *      or the authorization server rejected your request
     * @throws RequestExecutionException if trouble processing the request
     * @throws ResponseParsingException if trouble parsing the response
     */
    Fresh<AccessTokenResponse> requestAutoRefreshingToken(AccessTokenRequest request) 
            throws AccessTokenException, RequestExecutionException, ResponseParsingException;
    
    /**
     * POST to the token endpoint to get an always fresh HERE Access Token, for use with HERE Services.
     * The returned token is wrapped in a {@link Fresh}, and is periodically and automatically
     * refreshed before it expires.  The token can be used as an Authorization: Bearer token value.
     * See <a href="https://tools.ietf.org/html/rfc6749#section-7.1">OAuth2.0</a>, 
     * and <a href="https://tools.ietf.org/html/rfc6750">OAuth2.0 Bearer Token Usage</a> 
     * for details.
     *
     * @param requestSupplier a Supplier of token requests, to be used for each attempt to get a fresh token.
     * @return a {@link Fresh} wrapped Access Token that can be used as Bearer token for HERE Service requests
     *         the returned {@link Fresh} will always give an unexpired access token on a call to get()
     * @throws AccessTokenException if you had trouble authenticating your request to the authorization server, 
     *      or the authorization server rejected your request
     * @throws RequestExecutionException if trouble processing the request
     * @throws ResponseParsingException if trouble parsing the response
     */
    Fresh<AccessTokenResponse> requestAutoRefreshingToken(Supplier<AccessTokenRequest> requestSupplier) 
            throws AccessTokenException, RequestExecutionException, ResponseParsingException;

                                                                
}
