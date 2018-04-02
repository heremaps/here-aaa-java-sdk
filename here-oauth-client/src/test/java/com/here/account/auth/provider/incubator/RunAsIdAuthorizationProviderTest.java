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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.here.account.auth.provider.incubator.RunAsIdAuthorizationProvider;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpResponse;
import com.here.account.oauth2.HereAccessTokenProvider;

public class RunAsIdAuthorizationProviderTest {

    private RunAsIdAuthorizationProvider runAsIdAuthorizationProvider;

    String accessToken;
    HttpProvider httpProvider;
    
    @Before
    public void setUp() throws IOException, HttpException {
        httpProvider = Mockito.mock(HttpProvider.class);
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        
        accessToken = "h1.foo.bar";
        
        byte[] content = ("{\"access_token\":\""+accessToken+"\",\"expires_in\":120}").getBytes(StandardCharsets.UTF_8);
        long contentLength = content.length;
        InputStream inputStream = 
                new ByteArrayInputStream(
                content
                );
        Mockito.when(httpResponse.getStatusCode()).thenReturn(200);
        Mockito.when(httpResponse.getContentLength()).thenReturn(contentLength);
        Mockito.when(httpResponse.getResponseBody()).thenReturn(inputStream);
        Mockito.when(httpProvider.execute(Mockito.any())).thenReturn(httpResponse);

        runAsIdAuthorizationProvider = new RunAsIdAuthorizationProvider();

    }
    
    @Test
    public void test_url() {
        String tokenEndpointUrl = runAsIdAuthorizationProvider.getTokenEndpointUrl();
        String expectedTokenEndpointUrl = "http://localhost:8001/token";
        assertTrue("expected tokenEndpointUrl " + expectedTokenEndpointUrl + ", actual " + tokenEndpointUrl, 
                expectedTokenEndpointUrl.equals(tokenEndpointUrl));
    }
    
    @Test
    public void test_getTokenEndpointUrl_different() {
        String expectedTokenEndpointUrl = "http://www.example.com/token";
        runAsIdAuthorizationProvider = new RunAsIdAuthorizationProvider(expectedTokenEndpointUrl);
        String actualTokenEndpointUrl = runAsIdAuthorizationProvider.getTokenEndpointUrl();
        assertTrue("expected tokenEndpointUrl " + expectedTokenEndpointUrl
                + ", actual " + actualTokenEndpointUrl, 
                expectedTokenEndpointUrl.equals(actualTokenEndpointUrl));
    }
    
    @Test
    public void test_HereAccessTokenProvider() {
        HereAccessTokenProvider hereAccessTokenProvider = 
                HereAccessTokenProvider.builder()
                .setClientAuthorizationRequestProvider(runAsIdAuthorizationProvider)
                .setHttpProvider(httpProvider)
                .setAlwaysRequestNewToken(true)
                .build();
        String actualAccessToken = hereAccessTokenProvider.getAccessToken();
        assertTrue("actualAccessToken was null", null != actualAccessToken);
        assertTrue("actualAccessToken was blank", actualAccessToken.trim().length() > 0);
        
        assertTrue("expected accessToken " + accessToken + ", actual " + actualAccessToken, 
                accessToken.equals(actualAccessToken));
        

    }
    
}