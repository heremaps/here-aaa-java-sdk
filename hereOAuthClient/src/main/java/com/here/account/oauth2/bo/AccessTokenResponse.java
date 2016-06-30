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
