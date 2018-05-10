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
package com.here.account.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import com.here.account.auth.OAuth1Signer;
import com.here.account.auth.OAuth1SignerExposer;
import com.here.account.auth.provider.*;
import com.here.account.http.HttpProvider;
import com.here.account.util.Clock;
import com.here.account.util.SettableClock;
import com.here.account.util.SettableSystemClock;
import org.junit.Ignore;
import org.junit.Test;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import org.mockito.Mockito;

/**
 * @author kmccrack
 */
public class HereAccessTokenProviderIT {

    @Test
    public void test_builder_basic() throws IOException {
        try (
                HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder().build()
        ) {
            String accessToken = accessTokens.getAccessToken();
            assertTrue("accessToken was null", null != accessToken);
            assertTrue("accessToken was blank", accessToken.trim().length() > 0);

            AccessTokenResponse accessTokenResponse = accessTokens.getAccessTokenResponse();
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            assertEquals("tokenType invalid", "bearer", accessTokenResponse.getTokenType());
        }
    }

    private static final int ONE_HOUR_SKEW_MILLIS = 60 * 60 * 1000;

    @Test
    public void test_builder_clockskew_spy() throws IOException, NoSuchFieldException, IllegalAccessException {
        SettableSystemClock clock = new SettableSystemClock();
        ClientAuthorizationProviderChain provider = ClientAuthorizationProviderChain
                .getNewDefaultClientCredentialsProviderChain(clock);
        ClientAuthorizationRequestProvider mockProvider =  Mockito.spy(provider);

        OAuth1Signer oAuth1Signer = (OAuth1Signer) provider.getClientAuthorizer();
        String accessKeyId = OAuth1SignerExposer.getAccessKeyId(oAuth1Signer);
        String accessKeySecret = OAuth1SignerExposer.getAccessKeySecret(oAuth1Signer);
        clock.setCurrentTimeMillis(0L);
        HttpProvider.HttpRequestAuthorizer myAuthorizer = new OAuth1Signer(clock, accessKeyId, accessKeySecret);
        Mockito.doReturn(myAuthorizer).when(mockProvider).getClientAuthorizer();

        try (
                HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                        .setClientAuthorizationRequestProvider(mockProvider)
                        .setAlwaysRequestNewToken(true)
                        .build()
        ) {
            String accessToken = accessTokens.getAccessToken();
            assertTrue("accessToken was null", null != accessToken);
            assertTrue("accessToken was blank", accessToken.trim().length() > 0);

            AccessTokenResponse accessTokenResponse = accessTokens.getAccessTokenResponse();
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            assertEquals("tokenType invalid", "bearer", accessTokenResponse.getTokenType());
        }

    }

    @Test
    public void test_builder_clockskew() throws IOException, NoSuchFieldException, IllegalAccessException {
        try (
                HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                        .setAlwaysRequestNewToken(true)
                        .build()
        ) {
            for (int i = 0; i < 2; i++) {

                String accessToken = accessTokens.getAccessToken();
                assertTrue("accessToken was null", null != accessToken);
                assertTrue("accessToken was blank", accessToken.trim().length() > 0);

                AccessTokenResponse accessTokenResponse = accessTokens.getAccessTokenResponse();
                assertTrue("accessTokenResponse was null", null != accessTokenResponse);
                assertEquals("tokenType invalid", "bearer", accessTokenResponse.getTokenType());
            }
        }

    }


    @Test
    public void test_builder_basic_multipleTokens() throws IOException {
        do_builder_basic(10);
    }
    
    protected void do_builder_basic(int numTokens) throws IOException {
        try (
                HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder().build()
        ) {
            for (int i = 0; i < numTokens; i++) {
                String accessToken = accessTokens.getAccessToken();
                assertTrue("accessToken was null", null != accessToken);
                assertTrue("accessToken was blank", accessToken.trim().length() > 0);
            }
        }
    }

    
    @Ignore // we don't yet return credentials.ini files from our APIs
    @Test
    public void test_builder_ini() throws IOException {

        try (
                HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                .setClientAuthorizationRequestProvider(new FromHereCredentialsIniFile())
                .build()
        ) {
            String accessToken = accessTokens.getAccessToken();
            assertTrue("accessToken was null", null != accessToken);
            assertTrue("accessToken was blank", accessToken.trim().length() > 0);
        }
    }
    
    @Test
    public void test_builder_iniStream() throws IOException {
        Clock clock = new SettableSystemClock();
        try (
                InputStream inputStream = getTestIniFromOther();
                HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                .setClientAuthorizationRequestProvider(new FromHereCredentialsIniStream(clock, inputStream))
                .build()
        ) {
            String accessToken = accessTokens.getAccessToken();
            assertTrue("accessToken was null", null != accessToken);
            assertTrue("accessToken was blank", accessToken.trim().length() > 0);
        }
    }

    protected boolean notEmpty(String s) {
        return null != s && s.length() > 0;
    }

    /**
     * Using the file ~/.here/credentials.properties, create some fake credentials.ini 
     * content by prepending the default section in memory, returning an InputStream to it.
     * 
     * @return
     * @throws IOException
     */
    protected InputStream getTestIniFromOther() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write("[default]\n".getBytes(StandardCharsets.UTF_8));
            File file = FromDefaultHereCredentialsPropertiesFileExposer.getDefaultHereCredentialsFile();
            
            // System properties first
            String tokenEndpointUrl = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY);
            String accessKeyId = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY);
            String accessKeySecret = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY);
            if (notEmpty(tokenEndpointUrl) && notEmpty(accessKeyId) && notEmpty(accessKeySecret)) {
                outputStream.write((OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY 
                        + "=" + tokenEndpointUrl + "\n").getBytes(StandardCharsets.UTF_8));
                outputStream.write((OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY 
                        + "=" + accessKeyId + "\n").getBytes(StandardCharsets.UTF_8));
                outputStream.write((OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY 
                        + "=" + accessKeySecret + "\n").getBytes(StandardCharsets.UTF_8));
            } else {
                Properties properties = OAuth1ClientCredentialsProvider.getPropertiesFromFile(file);
                for (Entry<Object, Object> property : properties.entrySet()) {
                    Object name = property.getKey();
                    Object value = property.getValue();
                    String line = name + "=" + value + "\n";
                    outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                }
            }
            outputStream.flush();
            byte[] bytes = outputStream.toByteArray();
            System.out.println("configs:\n"+(new String(bytes, StandardCharsets.UTF_8))+"\n");
            return new ByteArrayInputStream(bytes);
        }
    }

    @Test
    public void test_alwaysRequestNewToken() throws IOException {
        try (
                HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                        .setAlwaysRequestNewToken(true)
                        .build()
        ) {
            String accessToken = accessTokens.getAccessToken();
            assertTrue("accessToken was null", null != accessToken);
            assertTrue("accessToken was blank", accessToken.trim().length() > 0);

            AccessTokenResponse accessTokenResponse = accessTokens.getAccessTokenResponse();
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            assertEquals("tokenType invalid", "bearer", accessTokenResponse.getTokenType());
        }
    }
}
