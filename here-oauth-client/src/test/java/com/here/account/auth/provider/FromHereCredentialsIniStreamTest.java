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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;

public class FromHereCredentialsIniStreamTest {

    // the constants are copied in this file to represent files that have already been issued, 
    // as canary tests to defend against unexpected changes in the code respect to file formats.
    
    static final String TEST_DEFAULT_INI_SECTION_NAME = "default";
    private static final String TEST_TOKEN_ENDPOINT_URL_PROPERTY = "here.token.endpoint.url";
    private static final String TEST_ACCESS_KEY_ID_PROPERTY = "here.access.key.id";
    private static final String TEST_ACCESS_KEY_SECRET_PROPERTY = "here.access.key.secret";

    private static final String SECTION_START = "[";
    private static final String SECTION_END = "]";
    private static final char NEWLINE = '\n';
    private static final char EQUALS = '=';
    
    private String tokenEndpointUrl = "tokenEndpointUrl";
    private String expectedTokenEndpointUrl = tokenEndpointUrl;
    private String accessKeyId = "accessKeyId";
    private String accessKeySecret = "accessKeySecret";
    
    FromHereCredentialsIniStream fromHereCredentialsIniStream;
    
    @Test(expected = NullPointerException.class)
    public void test_basic_null_stream() {
        fromHereCredentialsIniStream = new FromHereCredentialsIniStream(null);
    }
    
    @Test(expected = RuntimeException.class)
    public void test_basic_IOException_stream() {
        InputStream inputStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("socket broken");
            }
            
        };
        fromHereCredentialsIniStream = new FromHereCredentialsIniStream(inputStream);
    }

    protected byte[] getDefaultIniStreamContents() {
        StringBuilder buf = new StringBuilder()
                .append(SECTION_START)
                .append(TEST_DEFAULT_INI_SECTION_NAME)
                .append(SECTION_END)
                .append(NEWLINE)
                
                .append(TEST_TOKEN_ENDPOINT_URL_PROPERTY)
                .append(EQUALS)
                .append(tokenEndpointUrl)
                .append(NEWLINE)
                
                .append(TEST_ACCESS_KEY_ID_PROPERTY)
                .append(EQUALS)
                .append(accessKeyId)
                .append(NEWLINE)
                
                .append(TEST_ACCESS_KEY_SECRET_PROPERTY)
                .append(EQUALS)
                .append(accessKeySecret)
                .append(NEWLINE)
                ;
        
        return buf.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    @Test
    public void test_basic_default_stream() throws IOException {
       
        try (InputStream inputStream = new ByteArrayInputStream(
                getDefaultIniStreamContents()))
        {
            fromHereCredentialsIniStream = new FromHereCredentialsIniStream(inputStream);
            verifyExpected(fromHereCredentialsIniStream);
        }
    }
    
    protected void verifyExpected(ClientAuthorizationRequestProvider clientAuthorizationRequestProvider) {
        String actualTokenEndpointUrl = clientAuthorizationRequestProvider.getTokenEndpointUrl();
        assertTrue("tokenEndpointUrl expected "+expectedTokenEndpointUrl+", actual "+actualTokenEndpointUrl, 
                expectedTokenEndpointUrl.equals(actualTokenEndpointUrl));
        
        HttpRequestAuthorizer httpRequestAuthorizer = clientAuthorizationRequestProvider.getClientAuthorizer();
        assertTrue("httpRequestAuthorizer was null", null != httpRequestAuthorizer);
        
        // the authorizer must append an Authorization header in the OAuth scheme.
        HttpRequest httpRequest = mock(HttpRequest.class);

        String method = "GET";
        String url = "https://www.example.com/foo";
        Map<String, List<String>> formParams = null;
        httpRequestAuthorizer.authorize(httpRequest, method, url, formParams);

        verify(httpRequest, times(1)).addAuthorizationHeader(
                Mockito.matches("\\AOAuth .+\\z"));
    }
    
    @Test
    public void test_getHttpMethod() throws IOException {
        test_basic_default_stream();
        HttpMethods httpMethod = fromHereCredentialsIniStream.getHttpMethod();
        HttpMethods expectedHttpMethod = HttpMethods.POST;
        assertTrue("httpMethod expected " + expectedHttpMethod + ", actual " + httpMethod,
                expectedHttpMethod.equals(httpMethod));
    }

}
