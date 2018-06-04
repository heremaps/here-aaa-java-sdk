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

import com.here.account.auth.NoAuthorizer;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.util.Clock;
import com.here.account.util.JacksonSerializer;
import com.here.account.util.JsonSerializer;
import com.here.account.util.Serializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class HereAccessTokenProviderTest {

    HttpProvider mockHttpProvider;
    String expectedAccessToken;
    ClientAuthorizationRequestProvider clientAuthorizationRequestProvider;

    @Before
    public void setUp() throws IOException, HttpException {
        mockHttpProvider = Mockito.mock(HttpProvider.class);
        HttpProvider.HttpResponse httpResponse = Mockito.mock(HttpProvider.HttpResponse.class);
        expectedAccessToken = "ey789."+ UUID.randomUUID().toString()+".878";
        String responseBody = HereAccountTest.getResponseBody(expectedAccessToken);
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        Mockito.when(httpResponse.getStatusCode()).thenReturn(200);
        Mockito.when(httpResponse.getResponseBody()).thenReturn(new ByteArrayInputStream(bytes));
        Mockito.when(mockHttpProvider.execute(Mockito.any(HttpProvider.HttpRequest.class))).thenReturn(httpResponse);

        clientAuthorizationRequestProvider = new ClientAuthorizationRequestProvider() {
            @Override
            public String getTokenEndpointUrl() {
                return "https://www.example.com/oauth2/token";
            }

            @Override
            public HttpProvider.HttpRequestAuthorizer getClientAuthorizer() {
                return new NoAuthorizer();
            }

            @Override
            public AccessTokenRequest getNewAccessTokenRequest() {
                return new ClientCredentialsGrantRequest();
            }

            @Override
            public HttpConstants.HttpMethods getHttpMethod() {
                return HttpConstants.HttpMethods.POST;
            }

            @Override
            public Clock getClock() {
                return Clock.SYSTEM;
            }
        };

    }

    @Test
    public void test_HereAccessTokenProvider_getToken() throws IOException, HttpException {
        try (
            HereAccessTokenProvider hereAccessTokenProvider
                    = HereAccessTokenProvider.builder()
                    .setHttpProvider(mockHttpProvider)
                    .setClientAuthorizationRequestProvider(clientAuthorizationRequestProvider)
                    .build();
        ) {
            AccessTokenResponse accessTokenResponse = hereAccessTokenProvider.getAccessTokenResponse();
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            String accessToken = accessTokenResponse.getAccessToken();
            assertTrue("expected accessToken " + expectedAccessToken + ", actual " + accessToken,
                    expectedAccessToken.equals(accessToken));
        }
    }

    @Test
    public void test_HereAccessTokenProvider_alwaysRequest_getToken() throws IOException, HttpException {
        try (
                HereAccessTokenProvider hereAccessTokenProvider
                        = HereAccessTokenProvider.builder()
                        .setHttpProvider(mockHttpProvider)
                        .setClientAuthorizationRequestProvider(clientAuthorizationRequestProvider)
                        .setAlwaysRequestNewToken(true)
                        .build();
        ) {
            AccessTokenResponse accessTokenResponse = hereAccessTokenProvider.getAccessTokenResponse();
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            String accessToken = accessTokenResponse.getAccessToken();
            assertTrue("expected accessToken " + expectedAccessToken + ", actual " + accessToken,
                    expectedAccessToken.equals(accessToken));
        }
    }

    @Test
    public void test_HereAccessTokenProvider_defaultClientAuthorizationRequestProvider() throws IOException, HttpException {
        try (
                HereAccessTokenProvider hereAccessTokenProvider
                        = HereAccessTokenProvider.builder()
                        .setHttpProvider(mockHttpProvider)
                        .build();
        ) {
            AccessTokenResponse accessTokenResponse = hereAccessTokenProvider.getAccessTokenResponse();
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            String accessToken = accessTokenResponse.getAccessToken();
            assertTrue("expected accessToken " + expectedAccessToken + ", actual " + accessToken,
                    expectedAccessToken.equals(accessToken));
        }
    }

    @Test
    public void test_HereAccessTokenProvider_jsonSerializer() throws IOException, HttpException {
        Serializer jsonSerializer = Mockito.mock(Serializer.class);
        String tokenType = "bearer";
        Long expiresIn = 123L;
        String refreshToken = null;
        String idToken = null;
        AccessTokenResponse deserializedAccessTokenResponse = new AccessTokenResponse(
                 expectedAccessToken,
                 tokenType,
                 expiresIn,  refreshToken,  idToken
        );
        Mockito.when(jsonSerializer.jsonToPojo(Mockito.any(InputStream.class), Mockito.any(Class.class)))
                .thenReturn(deserializedAccessTokenResponse);

        try (
                HereAccessTokenProvider hereAccessTokenProvider
                        = HereAccessTokenProvider.builder()
                        .setHttpProvider(mockHttpProvider)
                        .setSerializer(jsonSerializer)
                        .build();
        ) {
            AccessTokenResponse accessTokenResponse = hereAccessTokenProvider.getAccessTokenResponse();
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            String accessToken = accessTokenResponse.getAccessToken();
            assertTrue("expected accessToken " + expectedAccessToken + ", actual " + accessToken,
                    expectedAccessToken.equals(accessToken));
        }
    }

    @Test
    public void test_HereAccessTokenProvider_defaultHttpProvider() throws IOException, HttpException {

        try (
                HereAccessTokenProvider hereAccessTokenProvider
                        = HereAccessTokenProvider.builder()
                        .setAlwaysRequestNewToken(true)
                        .build();
        ) {
            // we can't actually rely on the real HttpProvider/route working in a unit test
            // we could optionally launch our own mock service on localhost port, and connect to it
        }
    }



}
