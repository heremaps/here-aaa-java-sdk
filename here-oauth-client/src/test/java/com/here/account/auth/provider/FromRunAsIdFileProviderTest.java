/*
 * Copyright (c) 2018 HERE Europe B.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.here.account.auth.provider;

import static org.junit.Assert.assertTrue;

import com.here.account.http.HttpProvider;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.oauth2.AccessTokenRequest;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FromRunAsIdFileProviderTest {

    final String expectedTokenEndpointUrl = "file:///dev/shm/identity/access-token";
    final String expectedGrantType = "identity";

    @Test
    public void test_defaultConstructor() {
        FromRunAsIdFileProvider provider = new FromRunAsIdFileProvider();
        String tokenEndpointUrl = provider.getTokenEndpointUrl();

        assertTrue("expected tokenEndpointUrl " + expectedTokenEndpointUrl
                + ", actual " + tokenEndpointUrl,
                expectedTokenEndpointUrl.equals(tokenEndpointUrl));
    }

    FromRunAsIdFileProvider provider;
    
    @Test
    public void test_IdentityAuthorizationFileProvider() {
        provider = new FromRunAsIdFileProvider();
        verifyExpected(provider);
    }


    protected void verifyExpected(ClientAuthorizationRequestProvider clientCredentialsProvider) {
        String actualTokenEndpointUrl = clientCredentialsProvider.getTokenEndpointUrl();
        assertTrue("tokenEndpointUrl expected "+expectedTokenEndpointUrl+", actual "+actualTokenEndpointUrl,
                expectedTokenEndpointUrl.equals(actualTokenEndpointUrl));

        HttpProvider.HttpRequestAuthorizer httpRequestAuthorizer = clientCredentialsProvider.getClientAuthorizer();
        assertTrue("httpRequestAuthorizer was null", null != httpRequestAuthorizer);

        // the authorizer must append an Authorization header in the OAuth scheme.
        HttpProvider.HttpRequest httpRequest = Mockito.mock(HttpProvider.HttpRequest.class);

        String method = "GET";
        String url = "https://www.example.com/foo";
        Map<String, List<String>> formParams = null;
        httpRequestAuthorizer.authorize(httpRequest, method, url, formParams);

        Mockito.verify(httpRequest, Mockito.times(0)).addAuthorizationHeader(
                Mockito.matches("\\AOAuth .+\\z"));

        AccessTokenRequest request = clientCredentialsProvider.getNewAccessTokenRequest();
        assertTrue("Grant Type is not equal to " + expectedGrantType , request.getGrantType() == expectedGrantType);
    }
    
    @Test
    public void test_getHttpMethod() throws IOException {
        test_IdentityAuthorizationFileProvider();
        HttpMethods httpMethod = provider.getHttpMethod();
        HttpMethods expectedHttpMethod = HttpMethods.GET;
        assertTrue("httpMethod expected " + expectedHttpMethod + ", actual " + httpMethod,
                expectedHttpMethod.equals(httpMethod));
    }

}
