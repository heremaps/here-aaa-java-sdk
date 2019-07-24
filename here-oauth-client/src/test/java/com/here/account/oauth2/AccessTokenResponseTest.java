/*
 * Copyright (c) 2016 HERE Europe B.V.
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

import com.here.account.auth.OAuth1Signer;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.util.Clock;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;

public class AccessTokenResponseTest extends AbstractCredentialTezt{

    @Test
    public void testIdTokenIsSetViaConstructor() {
        String expectedAccessToken = "testAccessToken";
        String expectedTokenType = "testType";
        long expectedExpiresIn = 1200L;
        String expectedRefreshToken = "testRefreshToken";
        String expectedIdToken = "idToken";
        String expectedScope = "scope123";
        AccessTokenResponse response = new AccessTokenResponse(expectedAccessToken,
                expectedTokenType, expectedExpiresIn, expectedRefreshToken, expectedIdToken, expectedScope);

        assertEquals(response.getIdToken(), expectedIdToken);
    }

    @Test
    public void test_nonJSON_response_from_HA() throws IOException, HttpException {
        OAuth1Signer mockOauth1Signer = new OAuth1Signer(accessKeyId, accessKeySecret);
        String body = createMockHttpResponseBody();

        ClientCredentialsProvider mockClientCredentialsProvider = Mockito.mock(ClientCredentialsProvider.class);
        Mockito.doReturn(Clock.SYSTEM)
                .when(mockClientCredentialsProvider).getClock();
        Mockito.doReturn("https://www.example.com/oauth2/token")
                .when(mockClientCredentialsProvider).getTokenEndpointUrl();
        Mockito.doReturn(mockOauth1Signer)
                .when(mockClientCredentialsProvider).getClientAuthorizer();
        Mockito.doReturn(HttpConstants.HttpMethods.POST)
                .when(mockClientCredentialsProvider).getHttpMethod();

        HttpProvider.HttpRequest mockHttpRequest = Mockito.mock(HttpProvider.HttpRequest.class);
        Mockito.doNothing()
                .when(mockHttpRequest).addHeader(Mockito.anyString(), Mockito.anyString());

        HttpProvider mockHttpProvider = Mockito.mock(HttpProvider.class);
        Mockito.when(mockHttpProvider.getRequest(Mockito.any(HttpProvider.HttpRequestAuthorizer.class), anyString(),
                anyString(), Mockito.any(Map.class)))
                .thenReturn(mockHttpRequest);
        final HttpProvider.HttpResponse mockHttpResponse = new HttpProvider.HttpResponse() {
            @Override
            public int getStatusCode() {
                return HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED;
            }

            @Override
            public long getContentLength() {
                return body.getBytes(StandardCharsets.UTF_8).length;
            }

            @Override
            public Map<String, List<String>> getHeaders() {
                Map<String, List<String>> responseHeader = new HashMap<String, List<String>>();
                List<String> responseTypes = new ArrayList<String>();
                responseTypes.add("text/html");
                responseHeader.put(HttpConstants.CONTENT_TYPE, responseTypes);
                return responseHeader;
            }

            @Override
            public InputStream getResponseBody() {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                return new ByteArrayInputStream(bytes);
            }
        };
        Mockito.when(mockHttpProvider.execute(Mockito.any())).thenReturn(mockHttpResponse);

        AccessTokenRequest accessTokenRequest = new ClientCredentialsGrantRequest();
        accessTokenRequest.setAdditionalHeaders(Collections.singletonMap("testKey", "testValue"));
        accessTokenRequest.setExpiresIn(1L);

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(mockHttpProvider, mockClientCredentialsProvider);

        // expect the request to throw an exception, then validate the exception contents
        try {
            tokenEndpoint.requestToken(accessTokenRequest);
        } catch (AccessTokenException ate) {
            assertEquals("Expected proxyAuthenticationRequired error code",
                    HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED, ate.getStatusCode());
            assertEquals("Expected proxyAuthenticationRequired error code",
                    HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED, ate.getErrorResponse().getHttpStatus().intValue());
            assertEquals("Expected text in error response message field", body, ate.getErrorResponse().getMessage());
            return;
        }
        fail("Non-JSON response exception not thrown");
    }

    private String createMockHttpResponseBody() {
        return "<html>\n" +
                "<header><title>This is an error response</title></header>\n" +
                "<body>\n" +
                "Error response\n" +
                "</body>\n" +
                "</html>";
    }

    @Test
    public void testGetSetCorrelationId() {
        String expectedCorrelationId = "testCorrelationId";
        AccessTokenResponse response = new AccessTokenResponse();
        response.setCorrelationId(expectedCorrelationId);
        assertEquals(expectedCorrelationId, response.getCorrelationId());
    }
}
