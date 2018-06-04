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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.here.account.util.RefreshableResponseProvider.ExpiringResponse;

/**
 * Represents a parsed response received from an OAuth2.0 token endpoint. See the OAuth2.0 
 * <a href="https://tools.ietf.org/html/rfc6749#section-5.1">Successful Response</a> 
 * section for details.
 * 
 * @author kmccrack
 *
 */
public class AccessTokenResponse implements ExpiringResponse {

    /**
     * access_token
         REQUIRED.  The access token issued by the authorization server.
     */
    @JsonProperty("access_token")
    private final String accessToken;
    
    /**
     * token_type
         REQUIRED.  The type of the token issued as described in
         Section 7.1.  Value is case insensitive.
     */
    @JsonProperty("token_type")
    private final String tokenType;

    /**
     * expires_in
         RECOMMENDED.  The lifetime in seconds of the access token.  For
         example, the value "3600" denotes that the access token will
         expire in one hour from the time the response was generated.
         If omitted, the authorization server SHOULD provide the
         expiration time via other means or document the default value.
     */
    @JsonProperty("expires_in")
    private final Long expiresIn;
    
    /**
     * refresh_token
         OPTIONAL.  The refresh token, which can be used to obtain new
         access tokens using the same authorization grant as described
         in Section 6.
     */
    @JsonProperty("refresh_token")
    private final String refreshToken;
    
    /**
     * The start time in milliseconds, for this object, at the time it was 
     * constructed.
     */
    private final Long startTimeMilliseconds;

    @JsonProperty("id_token")
    private final String idToken;

    public AccessTokenResponse() {
        this(null, null, null, null,  null);
    }
    
    public AccessTokenResponse(String accessToken, 
            String tokenType,
            Long expiresIn, String refreshToken, String idToken) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.startTimeMilliseconds = System.currentTimeMillis();
        this.idToken = idToken;
    }

    /**
     * HERE Access Token.
     * 
     * <p> 
     * From OAuth2.0 
     * access_token
         REQUIRED.  The access token issued by the authorization server.
     *
     * @return the access_token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * The returned type of the token.
     * 
     * <p> 
     * From OAuth2.0 
     * token_type
         REQUIRED.  The type of the token issued as described in
         Section 7.1.  Value is case insensitive.
     * 
     * @return the token_type
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Seconds until expiration, at time of receipt of this object.
     * 
     * <p> 
     * From OAuth2.0 
     * expires_in
         RECOMMENDED.  The lifetime in seconds of the access token.  For
         example, the value "3600" denotes that the access token will
         expire in one hour from the time the response was generated.
         If omitted, the authorization server SHOULD provide the
         expiration time via other means or document the default value.
     * 
     * @return the expires_in
     */
    public Long getExpiresIn() {
       return expiresIn;
    }

    /**
     * If non-null, the refreshToken allows you to re-authorize and get a 
     * new fresh accessToken.
     * Remember, client_credentials grants never return a refresh token.
     * Resource owner password grants, and refresh token grants, sometimes do.
     * 
     * 
     * <p> 
     * From OAuth2.0 
     * refresh_token
         OPTIONAL.  The refresh token, which can be used to obtain new
         access tokens using the same authorization grant as described
         in Section 6.
     * 
     * @return the refresh_token
     */
    public String getRefreshToken() {
        return refreshToken;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Long getStartTimeMilliseconds() {
        return startTimeMilliseconds;
    }

    public String getIdToken() {
        return idToken;
    }

}
