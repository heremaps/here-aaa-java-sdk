package com.here.account.oauth2;

import java.io.IOException;

import org.junit.Test;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.bo.ClientCredentialsGrantRequest;

public class ClientCredentialsTest extends AbstractCredentialTezt {

    @Test
    public void test_javadocs_sample() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
         // set up urlStart, clientId, and clientSecret.
         SignIn signIn = HereAccessTokenProviders.getSignIn(
              ApacheHttpClientProvider.builder().build(), 
              urlStart, clientId, clientSecret);
         String hereAccessToken = signIn.signIn(
              new ClientCredentialsGrantRequest()).getAccessToken();
         // use hereAccessToken on requests until expires...
    }
}
