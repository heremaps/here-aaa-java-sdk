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
package com.here.account.oauth2.bo;

import com.here.account.util.RefreshableResponseProvider.ExpiringResponse;

public class AccessTokenResponse implements ExpiringResponse {

    private final String accessToken;
    private final Long expiresIn;
    private final Long startTimeMilliseconds;
    private final String refreshToken;
    
    public AccessTokenResponse() {
        this(null, null, null);
    }
    
    public AccessTokenResponse(String accessToken, 
            Long expiresIn, String refreshToken) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.startTimeMilliseconds = System.currentTimeMillis();
    }

    /**
     * HERE Access Token.
     * 
     * @return
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Seconds until expiration, at time of receipt of this object.
     * 
     * @return
     */
    public Long getExpiresIn() {
       return expiresIn;
    }

    /**
     * Current time milliseconds UTC at time of receipt of this object.
     * 
     * @return
     */
    public Long getStartTimeMilliseconds() {
        return startTimeMilliseconds;
    }

    /**
     * If non-null, the refreshToken allows you to re-authorize and get a 
     * new fresh accessToken.
     * Remember, client_credentials grants never return a refresh token.
     * Resource owner password grants, and refresh token grants, sometimes do.
     * 
     * @return the refreshToken
     */
    public String getRefreshToken() {
        return refreshToken;
    }
        
}
