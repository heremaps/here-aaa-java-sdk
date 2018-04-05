package com.here.account.oauth2;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.here.account.auth.OAuth2Authorizer;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;

public class AccessTokenAsOAuth2AuthorizerIT {

    /**
     * An enhancement to the JavadocsIT, this test uses the authorizer and 
     * verifies the access token it appended matched expectation.
     * 
     * @throws IOException
     */
    @Test
    public void test_HereAccessTokenProviderAsOAuth2AuthorizerSupplier() throws IOException {
        try (
            // use your provided System properties, ~/.here/credentials.ini, or credentials.properties file
            HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder().build()
        ) {
            OAuth2Authorizer authorizer = new OAuth2Authorizer(() -> {
                return accessTokens.getAccessToken();
            });
            // use the always-fresh authorizer on requests...
            
            useAuthorizer(authorizer);
        }
    }
    
    protected void useAuthorizer(HttpProvider.HttpRequestAuthorizer authorizer) {
        final String expectedBearerSpace = "Bearer ";
        HttpRequest httpRequest = new HttpRequest() {

            @Override
            public void addAuthorizationHeader(String value) {
                assertTrue("AuthorizationHeader didn't start with " + expectedBearerSpace, 
                        null != value && value.startsWith(expectedBearerSpace));
                // usually, this gets appended to a request to a Resource Server
                // but for this tutorial, we just display the Access Token.
                String accessToken = value.substring(expectedBearerSpace.length());
                verifyAccessToken(accessToken);
            }
            
        };
        String method = "GET";
        String url = "https://www.example.com/bar";
        Map<String, List<String>> formParams = null;
        authorizer.authorize(httpRequest, method, url, formParams);
    }
    
    protected void verifyAccessToken(String accessToken) {
        assertTrue("accessToken was null", null != accessToken);
        accessToken = accessToken.trim();
        assertTrue("accessToken was too short: " + accessToken.length(), accessToken.length() > 5);
    }

}
