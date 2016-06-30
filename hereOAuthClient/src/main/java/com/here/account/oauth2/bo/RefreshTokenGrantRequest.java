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
