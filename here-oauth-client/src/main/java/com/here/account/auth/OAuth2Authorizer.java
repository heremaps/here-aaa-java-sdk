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
package com.here.account.auth;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;

/**
 * Appends the specified 
 * <a href="https://tools.ietf.org/html/rfc6750">OAuth2.0 Bearer Token</a>
 * to the HTTP request.
 * 
 * <p>
 * See also 
 * <a href="http://www.iana.org/assignments/http-authschemes/http-authschemes.xhtml#authschemes">
 * HTTP Authentication Scheme Registry</a> for a list of authschemes.
 * 
 * @author kmccrack
 *
 */
public class OAuth2Authorizer implements HttpProvider.HttpRequestAuthorizer {
    
    private static final String BEARER_SPACE = "Bearer ";
    
    private final String bearerSpaceAccessToken;
    private final Supplier<String> accessTokenSupplier;
    
    /**
     * Construct the Bearer authorizer with the specified <tt>accessToken</tt>.
     * Access Token is as defined in 
     * <a href="https://tools.ietf.org/html/rfc6749#section-1.4">OAuth2.0 
     * Section 1.4</a>.
     * 
     * @param accessToken the OAuth2.0 Bearer Access Token value
     */
    public OAuth2Authorizer(String accessToken) {
        this.bearerSpaceAccessToken = BEARER_SPACE + accessToken;
        this.accessTokenSupplier = null;
    }
    
    /**
     * Construct the Bearer authorizer with the specified <tt>accessTokenSupplier</tt>.
     * This allows the <tt>accessTokenSupplier</tt> to change the Access Token it uses 
     * as needed, such as to avoid token expiration.
     * Access Token is as defined in 
     * <a href="https://tools.ietf.org/html/rfc6749#section-1.4">OAuth2.0 
     * Section 1.4</a>.
     *  
     * @param accessTokenSupplier the Supplier for 
     *      the OAuth2.0 Bearer Access Token values
     */
    public OAuth2Authorizer(Supplier<String> accessTokenSupplier) {
        this.bearerSpaceAccessToken = null;
        this.accessTokenSupplier = accessTokenSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authorize(HttpRequest httpRequest, String method, String url, Map<String, List<String>> formParams) {
        if (null != bearerSpaceAccessToken) {
            httpRequest.addAuthorizationHeader(bearerSpaceAccessToken);
        } else {
            httpRequest.addAuthorizationHeader(BEARER_SPACE + accessTokenSupplier.get());
        }
    }

}
