package com.here.account.oauth2;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.util.RefreshableResponseProvider;

public class RefreshableTokenAuthenticationProviderTest extends AbstractCredentialTezt {

    HttpProvider httpProvider;
    RefreshableResponseProvider<AccessTokenResponse> provider;
    
    @Before
    public void setUp() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        super.setUp();
        
        httpProvider = ApacheHttpClientProvider.builder()
        .setConnectionTimeoutInMs(HttpConstants.DEFAULT_CONNECTION_TIMEOUT_IN_MS)
        .setRequestTimeoutInMs(HttpConstants.DEFAULT_REQUEST_TIMEOUT_IN_MS)
        .build();

        this.provider = (RefreshableResponseProvider<AccessTokenResponse>) HereClientCredentialsGrantProviders
                .getRefreshableClientCredentialsProvider(
                        httpProvider,
                        urlStart, clientId, clientSecret, 
                100L);
    }
    
    protected static void verifyLoopChanges(RefreshableResponseProvider<AccessTokenResponse> signedIn) throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        String hereAccessToken = signedIn.getUnexpiredResponse().getAccessToken();
        assertTrue("hereAccessToken was null or blank", null != hereAccessToken && hereAccessToken.length() > 0);

        Set<String> allAccessTokens = new HashSet<String>();
        allAccessTokens.add(hereAccessToken);
        for (int i = 0; i < 3; i++) {
            String newAccessToken = verifyChanges(signedIn, hereAccessToken);
            assertTrue("newAccessToken wasn't new", allAccessTokens.add(newAccessToken));
            hereAccessToken = newAccessToken;
        }
        
        // test a blocking call
        try {
            Thread.sleep(2001L);
        } catch (InterruptedException e) {
            assertTrue("interrupted: "+e, false);
        }
        String newAccessToken = verifyChanges(signedIn, hereAccessToken);
        assertTrue("newAccessToken wasn't new", allAccessTokens.add(newAccessToken));

    }

    protected static String verifyChanges(
            RefreshableResponseProvider<AccessTokenResponse> signedIn, String hereAccessToken) throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        long expires = System.currentTimeMillis() + 10*1000L;
        String newAccessToken = hereAccessToken;
        int countSame = 0;
        while(System.currentTimeMillis() < expires && hereAccessToken.equals(newAccessToken)) {
            newAccessToken = signedIn.getUnexpiredResponse().getAccessToken();
            if (hereAccessToken.equals(newAccessToken)) {
                countSame++;
            }
        }
        System.out.println(countSame + " calls using same Here Access Token");
        assertTrue("newAccessToken hadn't changed", 
                newAccessToken != null && newAccessToken.length() > 0 && !hereAccessToken.equals(newAccessToken));
        return newAccessToken;
    }



    
    @Test
    public void test_10tokens() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        verifyLoopChanges(provider);
    }
    
    @After
    public void tearDown() throws IOException {
        try {
            if (null != httpProvider) {
                httpProvider.close();
            } 
        } finally {
            if (null != provider) {
                this.provider.close();
            }
        }
    }
}
