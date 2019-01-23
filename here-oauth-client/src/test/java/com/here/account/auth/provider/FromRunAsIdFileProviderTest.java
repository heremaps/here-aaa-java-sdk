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
import static org.junit.Assert.fail;

import com.here.account.http.HttpProvider;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.oauth2.AccessTokenRequest;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.util.Clock;
import com.here.account.util.SettableSystemClock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FromRunAsIdFileProviderTest {
    @Rule
    public TestName testName = new TestName();


    String expectedTokenEndpointUrl = "file:///dev/shm/identity/access-token";
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
    public void test_IdentityAuthorizationFileProvider() throws IOException {
        File file = File.createTempFile(testName.getMethodName(), "");
        file.deleteOnExit();
        String tokenUrl = FILE_URL_START + file.getAbsolutePath();
        provider = new FromRunAsIdFileProvider(new SettableSystemClock(), tokenUrl);
        this.expectedTokenEndpointUrl = tokenUrl;
        verifyExpected(provider);
    }

    @Test
    public void test_fileDoesNotExist() throws IOException {
        String testMethodName = testName.getMethodName();
        File file = new File(UUID.randomUUID().toString());
        String tokenUrl = FILE_URL_START + file.getAbsolutePath();
        provider = new FromRunAsIdFileProvider(new SettableSystemClock(), tokenUrl);
        this.expectedTokenEndpointUrl = tokenUrl;
        try {
            verifyExpected(provider);
            fail(testMethodName + "() should have thrown an exception, but didn't");
        } catch (RequestProviderException e) {
            assertTrue(e.getMessage().contains("does not exist"));
        }
    }

    @Test
    public void test_fileIsNotReadable() throws IOException {
        String testMethodName = testName.getMethodName();
        File file = File.createTempFile(testMethodName, "");
        file.deleteOnExit();
        file.setReadable(false);
        String tokenUrl = FILE_URL_START + file.getAbsolutePath();
        provider = new FromRunAsIdFileProvider(new SettableSystemClock(), tokenUrl);
        this.expectedTokenEndpointUrl = tokenUrl;
        try {
            verifyExpected(provider);
            fail(testMethodName + "() should have thrown an exception, but didn't");
        } catch (RequestProviderException e) {
            assertTrue(e.getMessage().contains("is not readable"));
        }
    }



    static final String FILE_URL_START = "file://";

    @Test
    public void test_inchain() throws IOException {
        Clock clock = new SettableSystemClock();
        File file = new File(UUID.randomUUID().toString());
        String tokenUrl = FILE_URL_START + file.getAbsolutePath();
        provider = new FromRunAsIdFileProvider(clock, tokenUrl);
        File file2 = File.createTempFile(testName.getMethodName(), "");
        file2.deleteOnExit();
        String tokenUrl2 = FILE_URL_START + file2.getAbsolutePath();
        ClientAuthorizationRequestProvider provider2 = new FromRunAsIdFileProvider(clock, tokenUrl2);
        ClientAuthorizationRequestProvider providerChain = new ClientAuthorizationProviderChain(provider, provider2);
        this.expectedTokenEndpointUrl = tokenUrl2;
        // expected will only match if the providerChain successfully passed over provider,
        // because the File didn't exist, and the providerChain chose provider2
        verifyExpected(providerChain);
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
