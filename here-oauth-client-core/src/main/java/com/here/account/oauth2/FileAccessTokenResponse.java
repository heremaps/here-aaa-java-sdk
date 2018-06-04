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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A FileAccessTokenResponse provides access to an Access Token 
 * read from a File.  Because Files may have been written in the 
 * past, the method {@link #getExp()} gives you the proper expiration time, 
 * and {@link #getExpiresIn()} is dynamically computed to behave as though 
 * you just got the response from a server.
 * 
 * @author kmccrack
 *
 */
public class FileAccessTokenResponse extends AccessTokenResponse {

    /**
     * exp
         The "exp" (expiration time) claim identifies the expiration time on
   or after which the JWT MUST NOT be accepted for processing.
     *
     * <p>
     * See also 
     * <a href="https://tools.ietf.org/html/rfc7519#section-4.1.4">JSON Web Token (JWT):  &quot;exp&quot; (Expiration Time) Claim</a>.
     * 
     * <p>
     * It is of type "NumericDate": 
     *   A JSON numeric value representing the number of seconds from
      1970-01-01T00:00:00Z UTC until the specified UTC date/time,
      ignoring leap seconds.
     * 
     * <p>
     * See also 
     * <a href="https://tools.ietf.org/html/rfc7519#section-2">JSON Web Token (JWT):  Terminology</a>.
     */
    @JsonProperty("exp")
    private final Long exp;
    
    public FileAccessTokenResponse() {
        this(null, null, null, null,  null, null);
    }
    
    public FileAccessTokenResponse(String accessToken, 
            String tokenType,
            Long expiresIn, String refreshToken, String idToken,
            Long exp) {
        super(accessToken, 
            tokenType,
            expiresIn, refreshToken, idToken);
        
        this.exp = exp;
    }
    
    /**
     * In a File-based Access Token Response, the access_token may have been written 
     * long ago, and the appropriate expiresIn value is derived from the fixed quantity 
     * "exp" seconds minus the time of this object creation in seconds.
     * 
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Long getExpiresIn() {
        return exp - (getStartTimeMilliseconds() / 1000);
    }

    /**
     * exp
         The "exp" (expiration time) claim identifies the expiration time on
   or after which the JWT MUST NOT be accepted for processing.
     *
     * <p>
     * See also 
     * <a href="https://tools.ietf.org/html/rfc7519#section-4.1.4">JSON Web Token (JWT):  &quot;exp&quot; (Expiration Time) Claim</a>.
     * 
     * <p>
     * It is of type "NumericDate": 
     *   A JSON numeric value representing the number of seconds from
      1970-01-01T00:00:00Z UTC until the specified UTC date/time,
      ignoring leap seconds.
     * 
     * <p>
     * See also 
     * <a href="https://tools.ietf.org/html/rfc7519#section-2">JSON Web Token (JWT):  Terminology</a>.
     * 
     * @return the exp
     */
    public Long getExp() {
        return exp;
    }

}