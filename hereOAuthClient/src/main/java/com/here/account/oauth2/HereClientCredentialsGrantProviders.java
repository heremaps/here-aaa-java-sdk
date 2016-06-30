package com.here.account.oauth2;

import java.io.IOException;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.ClientCredentialsGrantRequest;
import com.here.account.oauth2.bo.PasswordGrantRequest;
import com.here.account.util.RefreshableResponseProvider;

/**
 * See the OAuth2.0 
 * <a href="https://tools.ietf.org/html/rfc6749#section-4.4">Client Credentials Grant</a>.
 * 
 * @author kmccrack
 *
 */
public class HereClientCredentialsGrantProviders {

    /**
     * Get the ability to run various Obtaining Authorization API calls to the 
     * HERE Account Authorization Server.
     * See OAuth2.0 
     * <a href="https://tools.ietf.org/html/rfc6749#section-4">Obtaining Authorization</a>.
     * 
     * @param httpProvider
     * @param urlStart
     * @param clientId
     * @param clientSecret
     * @return
     */
    public static SignIn getSignIn(
            HttpProvider httpProvider,
            String urlStart, String clientId, String clientSecret) {
        return
                new SignIn( httpProvider,  urlStart,  clientId,  clientSecret 
                        );
    }
    
    public static RefreshableResponseProvider<AccessTokenResponse> getRefreshableUserCredentialsProvider(
            HttpProvider httpProvider,
            String urlStart, String clientId, String clientSecret, 
            String email, String password,
            Long optionalRefreshInterval
            ) throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        SignIn signIn = 
                getSignIn( httpProvider,  urlStart,  clientId,  clientSecret 
                        );
        PasswordGrantRequest passwordGrantRequest = new PasswordGrantRequest().setEmail(email).setPassword(password);
        AccessTokenResponse accessTokenResponse = 
                signIn.signIn(passwordGrantRequest);
        Long refreshIntervalMillis = null;
        RefreshableResponseProvider<AccessTokenResponse> refreshableResponseProvider 
        = new RefreshableResponseProvider<AccessTokenResponse>(
                refreshIntervalMillis,
                accessTokenResponse,
                new UserCredentialsRefresher(signIn)
                );
        return refreshableResponseProvider;
    }

    public static RefreshableResponseProvider<AccessTokenResponse> getRefreshableClientCredentialsProvider(
            HttpProvider httpProvider,
            String urlStart, String clientId, String clientSecret, 
            Long optionalRefreshInterval) throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        SignIn signIn = 
                getSignIn( httpProvider,  urlStart,  clientId,  clientSecret 
                        );
        /*      final Long refreshIntervalMillis,
      final T initialToken,
      final ResponseRefresher<T> refreshTokenFunction
*/
        return new RefreshableResponseProvider<AccessTokenResponse>(
                optionalRefreshInterval,
                signIn.signIn(new ClientCredentialsGrantRequest()),
                new ClientCredentialsRefresher(signIn));
    }
    
}
