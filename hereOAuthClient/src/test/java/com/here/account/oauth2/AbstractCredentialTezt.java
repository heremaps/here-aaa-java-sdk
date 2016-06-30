package com.here.account.oauth2;

import java.io.IOException;

import org.junit.Before;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;

public abstract class AbstractCredentialTezt {

    String urlStart;
    String clientId;
    String clientSecret;
    
    @Before
    public void setUp() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        urlStart = System.getProperty("urlStart");
        clientId = System.getProperty("clientId");
        clientSecret = System.getProperty("clientSecret");
    }

}
