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
package com.here.account.auth.provider.incubator;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.here.account.util.SettableSystemClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.here.account.auth.provider.ClientAuthorizationProviderChain;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.oauth2.HereAccessTokenProvider;
import com.here.account.util.JacksonSerializer;

public class HereAccessTokenProviderFromRunAsIdFileProviderTest {
    
    private final int ONE_DAY_IN_SECONDS = 24*60*60;

    Map<String, Object> fileAccessTokenResponse;
    String accessToken;
    JacksonSerializer serializer;
    File file;
    ClientAuthorizationRequestProvider clientAuthorizationRequestProvider;
    
    @Before
    public void setUp() throws IOException {
        String prefix = UUID.randomUUID().toString();
        String suffix = null;
        file = File.createTempFile(prefix, suffix);

        fileAccessTokenResponse = new HashMap<String, Object>();
        accessToken = "h1.test.value";
        fileAccessTokenResponse.put("access_token", accessToken);
        Long expOneDay = (System.currentTimeMillis() / 1000) + ONE_DAY_IN_SECONDS;
        fileAccessTokenResponse.put("exp", expOneDay);
        serializer = new JacksonSerializer();
        
        clientAuthorizationRequestProvider = 
                new FromRunAsIdFileProvider(
                        new SettableSystemClock(),
                        "file://" + file.getAbsolutePath());


    }
    
    @After
    public void tearDown() {
        if (null != file) {
            file.delete();
        }
    }
    
    @Test(expected=NullPointerException.class)
    public void test_no_exp() throws IOException {
        fileAccessTokenResponse.clear();
        fileAccessTokenResponse.put("access_token", accessToken);
        test_HereAccessTokenProvider_Identity();
    }
    
    @Test(expected=NullPointerException.class)
    public void test_null_exp_providerChain() throws IOException {
        fileAccessTokenResponse.clear();
        fileAccessTokenResponse.put("access_token", accessToken);
        
        clientAuthorizationRequestProvider = new ClientAuthorizationProviderChain(clientAuthorizationRequestProvider);
        test_HereAccessTokenProvider_Identity();
    }
    
    @Test
    public void test_HereAccessTokenProvider_Identity() throws IOException {
        try {
            String json = serializer.objectToJson(fileAccessTokenResponse);
            System.out.println(json);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = new FileOutputStream(file)) {
                out.write(bytes);
                out.flush();
            }
            
            HereAccessTokenProvider tokenProvider = HereAccessTokenProvider.builder().setClientAuthorizationRequestProvider(clientAuthorizationRequestProvider).build();
            String actualAccessToken = tokenProvider.getAccessToken();
            assertTrue("expected accessToken " + accessToken + ", actual accessToken " + actualAccessToken,
                    accessToken.equals(actualAccessToken));
        } finally {
            file.delete();
        }
    }
    
}
