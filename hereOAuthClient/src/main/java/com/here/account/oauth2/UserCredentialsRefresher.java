package com.here.account.oauth2;

import java.io.IOException;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.RefreshTokenGrantRequest;
import com.here.account.util.RefreshableResponseProvider.ResponseRefresher;

public class UserCredentialsRefresher implements ResponseRefresher<AccessTokenResponse> {
    
    private SignIn signIn;

    public UserCredentialsRefresher(SignIn signIn) {
        this.signIn = signIn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenResponse refresh(AccessTokenResponse previous) {
        RefreshTokenGrantRequest refreshTokenGrantRequest = new RefreshTokenGrantRequest()
                .setAccessToken(previous.getAccessToken())
                .setRefreshToken(previous.getRefreshToken());
        try {
            return signIn.signIn(refreshTokenGrantRequest);
        } catch (AuthenticationRuntimeException | IOException | AuthenticationHttpException | HttpException e) {
            throw new AuthenticationRuntimeException("trouble during Sign In: " + e, e);
        }
    }
    

}
