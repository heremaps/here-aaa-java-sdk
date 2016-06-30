package com.here.account.oauth2;

import java.io.IOException;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.ClientCredentialsGrantRequest;
import com.here.account.util.RefreshableResponseProvider.ResponseRefresher;

public class ClientCredentialsRefresher implements ResponseRefresher<AccessTokenResponse> {
    
    private SignIn signIn;
    
    public ClientCredentialsRefresher(SignIn signIn) {
        this.signIn = signIn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenResponse refresh(AccessTokenResponse previous) {
        try {
            return signIn.signIn(new ClientCredentialsGrantRequest());
        } catch (IOException | AuthenticationHttpException | HttpException e) {
            throw new AuthenticationRuntimeException("trouble refresh: " + e, e);
        }
    }
  
}
