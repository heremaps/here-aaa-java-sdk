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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.auth.provider.FromHereCredentialsIniFile;
import com.here.account.auth.provider.FromHereCredentialsIniStream;
import com.here.account.auth.provider.FromDefaultHereCredentialsPropertiesFileExposer;

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

        try (
                InputStream inputStream = getTestIniFromOther();
                HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                .setClientAuthorizationRequestProvider(new FromHereCredentialsIniStream(inputStream))
                .build()
        ) {
            String accessToken = accessTokens.getAccessToken();
            assertTrue("accessToken was null", null != accessToken);
            assertTrue("accessToken was blank", accessToken.trim().length() > 0);
        }
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
            if (null != tokenEndpointUrl && null != accessKeyId && null != accessKeySecret) {
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
            return new ByteArrayInputStream(bytes);
        }
    }

}
