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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For grant_type=refresh_token.
 * 
 * @author kmccrack
 *
 */
public class RefreshTokenGrantRequest extends AuthorizationRequest {
    
    private String accessToken;
    private String refreshToken;
    
    public RefreshTokenGrantRequest() {
        super("refresh_token");
    }
    
    public RefreshTokenGrantRequest setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }
    
    public RefreshTokenGrantRequest setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toJson() {
        return "{\"grantType\":\"" + getGrantType()
            + "\",\"accessToken\":\"" + getAccessToken()
            + "\",\"refreshToken\":\"" + getRefreshToken() + "\"}";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> toFormParams() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        addFormParam(formParams, "grant_type", getGrantType());
        addFormParam(formParams, "access_token", getAccessToken());
        addFormParam(formParams, "refresh_token", getRefreshToken());
        return formParams;
    }
    
    

}
