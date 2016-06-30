package com.here.account.oauth2;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.bo.ClientCredentialsGrantRequest;

public class SignInWithClientCredentialsTest extends AbstractCredentialTezt {

    HttpProvider httpProvider;
    SignIn signIn;
    
    @Before
    public void setUp() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        super.setUp();
        //    public SignInWithClientCredentials(String urlStart, String clientId, String clientSecret) {
        
        httpProvider = ApacheHttpClientProvider.builder()
        .setConnectionTimeoutInMs(HttpConstants.DEFAULT_CONNECTION_TIMEOUT_IN_MS)
        .setRequestTimeoutInMs(HttpConstants.DEFAULT_REQUEST_TIMEOUT_IN_MS)
        .build();
        
        this.signIn = new SignIn(
                httpProvider,
                urlStart, clientId, clientSecret
                );
    }
    
    @After
    public void tearDown() throws IOException {
        if (null != httpProvider) {
            httpProvider.close();
        }
    }

    @Test
    public void test_signIn() throws IOException, InterruptedException, ExecutionException, AuthenticationHttpException, AuthenticationRuntimeException, HttpException {
        String hereAccessToken = signIn.signIn(new ClientCredentialsGrantRequest()).getAccessToken();
        assertTrue("hereAccessToken was null or blank", null != hereAccessToken && hereAccessToken.length() > 0);
    }
}
