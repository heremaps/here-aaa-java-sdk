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

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpProvider;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.oauth2.ClientCredentialsGrantRequest;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ClientAuthorizationProviderChainTest {

    String expectedTokenEndpointUrl1 = "expectedTokenEndpointUrl1";
    String expectedAccessKeyId1 = "expectedAccessKeyId1";
    String expectedAccessKeySecret1 = "accessKeySecret1";

    String tokenEndpointUrl;
    String accessKeyId;
    String accessKeySecret;
    File file;

    protected void createTmpFile() throws IOException {
        String prefix = UUID.randomUUID().toString();
        file = File.createTempFile(prefix, null);
        file.deleteOnExit();
    }

    @After
    public void tearDown() {
        if (null != file) {
            file.delete();
        }
        restoreSystemProperties();
    }

    public ClientAuthorizationRequestProvider getClientAuthorizationRequestProviderFromIniFile() throws Exception {
        createTmpFile();
        FromHereCredentialsIniStreamTest test = new FromHereCredentialsIniStreamTest();
        byte[] bytes = test.getDefaultIniStreamContents();
        try (OutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
            outputStream.flush();
        }
        return new FromHereCredentialsIniFile(file, FromHereCredentialsIniStreamTest.TEST_DEFAULT_INI_SECTION_NAME);
    }

    public ClientAuthorizationRequestProvider getClientAuthorizationRequestProviderFromIniFileWithNoPropertiesSet() throws Exception {
        createTmpFile();
        return new FromHereCredentialsIniFile(file, FromHereCredentialsIniStreamTest.TEST_DEFAULT_INI_SECTION_NAME);
    }

    public ClientAuthorizationRequestProvider getClientAuthorizationRequestProviderFromSystemProperties() {
        tokenEndpointUrl = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY);
        accessKeyId = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY);
        accessKeySecret = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY);

        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY, expectedTokenEndpointUrl1);
        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY, expectedAccessKeyId1);
        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY, expectedAccessKeySecret1);

        return  new FromSystemProperties();
    }

    public ClientAuthorizationRequestProvider getClientAuthorizationRequestProviderFromSystemPropertiesWithNoPropertiesSet() {
        tokenEndpointUrl = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY);
        accessKeyId = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY);
        accessKeySecret = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY);

        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY, "");
        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY, "");
        System.setProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY, "");

        return  new FromSystemProperties();
    }


    public void restoreSystemProperties() {
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

    public ClientAuthorizationRequestProvider getClientAuthorizationRequestProviderFromHerePropertiesFile() throws Exception {
        return new FromDefaultHereCredentialsPropertiesFile();
    }

    public ClientAuthorizationRequestProvider getClientAuthorizationRequestProviderFromHerePropertiesFileWithContent() throws Exception {
        String prefix = UUID.randomUUID().toString();
        File file = File.createTempFile(prefix, null);
        file.deleteOnExit();
        
        byte[] bytes = ("here.token.endpoint.url=https://www.example.com/oauth2/token\n"
                + "here.client.id=my-client-id\n"
                + "here.access.key.id=my-access-key-id\n"
                + "here.access.key.secret=my-secret\n")
                .getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
            outputStream.flush();
        }

        return new FromDefaultHereCredentialsPropertiesFile(file);
    }

    @Test
    public void test_DefaultProviderChain() throws Exception{

        ClientAuthorizationRequestProvider fromSystemProperties = getClientAuthorizationRequestProviderFromSystemProperties();
        ClientAuthorizationRequestProvider fromIniFile = getClientAuthorizationRequestProviderFromIniFile();
        ClientAuthorizationRequestProvider fromPropertiesFile = getClientAuthorizationRequestProviderFromHerePropertiesFile();

        ClientAuthorizationProviderChain providerChain = new ClientAuthorizationProviderChain
                (fromSystemProperties, fromIniFile, fromPropertiesFile);

       verifyExpected(providerChain, fromSystemProperties);
    }

    @Test (expected = RequestProviderException.class)
    public void test_noProviderChain() {
        ClientAuthorizationProviderChain providerChain = new ClientAuthorizationProviderChain();
        providerChain.getClientCredentialsProvider();
    }

    @Test
    public void test_DefaultProviderChain_defaultingToIni() throws Exception{

        ClientAuthorizationRequestProvider fromSystemProperties = getClientAuthorizationRequestProviderFromSystemPropertiesWithNoPropertiesSet();
        ClientAuthorizationRequestProvider fromIniFile = getClientAuthorizationRequestProviderFromIniFile();
        ClientAuthorizationRequestProvider fromPropertiesFile = getClientAuthorizationRequestProviderFromHerePropertiesFile();

        ClientAuthorizationProviderChain providerChain = new ClientAuthorizationProviderChain
                (fromSystemProperties, fromIniFile, fromPropertiesFile);

        verifyExpected(providerChain, fromIniFile);
    }

    @Test
    public void test_DefaultProviderChain_defaultingToProperties() throws Exception{

        ClientAuthorizationRequestProvider fromSystemProperties = getClientAuthorizationRequestProviderFromSystemPropertiesWithNoPropertiesSet();
        ClientAuthorizationRequestProvider fromIniFile = getClientAuthorizationRequestProviderFromIniFileWithNoPropertiesSet();
        ClientAuthorizationRequestProvider fromPropertiesFile = getClientAuthorizationRequestProviderFromHerePropertiesFileWithContent();

        ClientAuthorizationProviderChain providerChain = new ClientAuthorizationProviderChain
                (fromSystemProperties, fromIniFile, fromPropertiesFile);

        verifyExpected(providerChain, fromPropertiesFile);
    }

    protected void verifyExpected(ClientAuthorizationProviderChain providerChain, ClientAuthorizationRequestProvider
                                  clientAuthorizationRequestProvider) {

        String expectedTokenEndpointUrl = clientAuthorizationRequestProvider.getTokenEndpointUrl();
        String actualTokenEndpointUrl = providerChain.getTokenEndpointUrl();

        assertTrue("tokenEndpointUrl expected "+expectedTokenEndpointUrl+", actual "+ actualTokenEndpointUrl,
                expectedTokenEndpointUrl.equals(actualTokenEndpointUrl));
        assertTrue("httpMethod was expected to be POST", providerChain.getHttpMethod() == HttpConstants.HttpMethods.POST);

        HttpProvider.HttpRequestAuthorizer httpRequestAuthorizer = providerChain.getClientAuthorizer();
        assertTrue("httpRequestAuthorizer was null", null != httpRequestAuthorizer);
        // the authorizer must append an Authorization header in the OAuth scheme.
        HttpProvider.HttpRequest httpRequest = mock(HttpProvider.HttpRequest.class);

        String method = "GET";
        String url = "https://www.example.com/foo";
        Map<String, List<String>> formParams = null;
        httpRequestAuthorizer.authorize(httpRequest, method, url, formParams);

        verify(httpRequest, times(1)).addAuthorizationHeader(
                Mockito.matches("\\AOAuth .+\\z"));
        assertTrue("grantType should equal " + ClientCredentialsGrantRequest.CLIENT_CREDENTIALS_GRANT_TYPE,
                providerChain.getNewAccessTokenRequest().getGrantType().equals(ClientCredentialsGrantRequest.CLIENT_CREDENTIALS_GRANT_TYPE));

    }
}
