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

import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class FromHereCredentialsIniStreamTest extends FromHereCredentialsIniConstants {

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

    @Test(expected = RuntimeException.class)
    public void test_invalid_stream() throws IOException {
        FromHereCredentialsIniStream.getPropertiesFromIni(null, TEST_DEFAULT_INI_SECTION_NAME);
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
