package com.here.account.auth.provider;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.here.account.http.HttpProvider;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FromSystemPropertiesTest {

    private static final String TEST_TOKEN_ENDPOINT_URL_PROPERTY = "here.token.endpoint.url";
    private static final String TEST_DEFAULT_TOKEN_ENDPOINT_URL = "https://account.api.here.com/oauth2/token";
    FromSystemProperties fromSystemProperties;
    
    String expectedTokenEndpointUrl = "expectedTokenEndpointUrl";
    String expectedAccessKeyId = "expectedAccessKeyId";
    String expectedAccessKeySecret = "accessKeySecret";
    
    String tokenEndpointUrl;
    String accessKeyId;
    String accessKeySecret;
    
    @Before
    public void setUp() {
        tokenEndpointUrl = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY);
        accessKeyId = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY);
        accessKeySecret = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY);

        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY, expectedTokenEndpointUrl);
        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY, expectedAccessKeyId);
        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY, expectedAccessKeySecret);
    }
    
    @After
    public void tearDown() {
        // there's no way to undo a System.setProperty(a, b) if a was previously null
        // the best we can do is a ""
        if (null == tokenEndpointUrl) {
            tokenEndpointUrl = "";
        }
        if (null == accessKeyId) {
            accessKeyId = "";
        }
        if (null == accessKeySecret) {
            accessKeySecret = "";
        }
        
        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY, tokenEndpointUrl);
        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY, accessKeyId);
        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY, accessKeySecret);

    }
    
    @Test
    public void test_properties() {
        fromSystemProperties = new FromSystemProperties();
        verifyExpected(fromSystemProperties);

    }

    protected void verifyExpected(ClientAuthorizationRequestProvider clientCredentialsProvider) {
        String actualTokenEndpointUrl = clientCredentialsProvider.getTokenEndpointUrl();
        assertTrue("tokenEndpointUrl expected "+expectedTokenEndpointUrl+", actual "+actualTokenEndpointUrl,
                expectedTokenEndpointUrl.equals(actualTokenEndpointUrl));

        HttpProvider.HttpRequestAuthorizer httpRequestAuthorizer = clientCredentialsProvider.getClientAuthorizer();
        assertTrue("httpRequestAuthorizer was null", null != httpRequestAuthorizer);

        // the authorizer must append an Authorization header in the OAuth scheme.
        HttpProvider.HttpRequest httpRequest = mock(HttpProvider.HttpRequest.class);

        String method = "GET";
        String url = "https://www.example.com/foo";
        Map<String, List<String>> formParams = null;
        httpRequestAuthorizer.authorize(httpRequest, method, url, formParams);

        verify(httpRequest, times(1)).addAuthorizationHeader(
                Mockito.matches("\\AOAuth .+\\z"));
    }
    
    @Test
    public void test_getHttpMethod() throws IOException {
        test_properties();
        HttpMethods httpMethod = fromSystemProperties.getHttpMethod();
        HttpMethods expectedHttpMethod = HttpMethods.POST;
        assertTrue("httpMethod expected " + expectedHttpMethod + ", actual " + httpMethod,
                expectedHttpMethod.equals(httpMethod));
    }

}
