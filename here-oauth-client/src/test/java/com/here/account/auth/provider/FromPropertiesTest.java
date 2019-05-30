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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.here.account.http.HttpConstants;
import com.here.account.http.HttpProvider;
import com.here.account.util.Clock;
import com.here.account.util.SettableSystemClock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

public class FromPropertiesTest {

    private final String tokenEndpointUrl = "https://www.example.com/token" + UUID.randomUUID();
    private final String accessKeyId = "my-access-key-id" + UUID.randomUUID();
    private final String accessKeySecret = "my-access-key-secret" + UUID.randomUUID();
    private final String scope = "my-scope-" + UUID.randomUUID();
    private Clock clock;
    private FromProperties fromProperties;

    @Before
    public void setUp() {
        this.clock = new SettableSystemClock();
    }

    @Test
    public void test_3props_constructor() {
        fromProperties = new FromProperties(clock, tokenEndpointUrl, accessKeyId, accessKeySecret);

        verifyFromProperties(false);
    }

    @Test
    public void test_4props_constructor() {
        fromProperties = new FromProperties(clock, tokenEndpointUrl, accessKeyId, accessKeySecret, scope);

        verifyFromProperties(true);
    }

    protected void verifyFromProperties(Boolean hasScopeProperty) {
        String actualTokenEndpointUrl = fromProperties.getTokenEndpointUrl();
        assertTrue("expected tokenEndpointUrl " + tokenEndpointUrl + ", actual " + actualTokenEndpointUrl,
                tokenEndpointUrl.equals(actualTokenEndpointUrl));

        String actualScope = fromProperties.getScope();
        if (hasScopeProperty) {
            assertTrue("expected scope " + scope + ", actual " + actualScope, scope.equals(actualScope));
        } else {
            assertNull("expected scope to be NULL, actual " + actualScope, actualScope);
        }


        HttpProvider.HttpRequestAuthorizer authorizer = fromProperties.getClientAuthorizer();
        assertTrue("authorizer was null", null != authorizer);
        HttpProvider.HttpRequest mockHttpRequest = Mockito.mock(HttpProvider.HttpRequest.class);

        String method = "POST";
        String url = tokenEndpointUrl;
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        List<String> value = Collections.singletonList("725");
        formParams.put("key", value);
        authorizer.authorize(mockHttpRequest, method, url, formParams);
        Mockito.verify(mockHttpRequest, Mockito.times(1)).addAuthorizationHeader(Mockito.any(String.class));

        HttpConstants.HttpMethods expectedMethod = HttpConstants.HttpMethods.POST;
        HttpConstants.HttpMethods method2 = fromProperties.getHttpMethod();
        assertTrue("expected method " + expectedMethod + " , actual " + method2,
                expectedMethod == method2);

    }

    @Test
    public void test_properties_constructor() {
        Properties properties = new Properties();

        properties.put("here.token.endpoint.url", tokenEndpointUrl);
        properties.put("here.access.key.id", accessKeyId);
        properties.put("here.access.key.secret", accessKeySecret);
        properties.put("here.token.scope", scope);

        fromProperties = new FromProperties(clock, properties);

        verifyFromProperties(true);
    }

    @Test(expected = NullPointerException.class) // OAuth1ClientCredentialsProvider complains
    public void test_properties_badid_constructor() {
        Properties properties = new Properties();

        properties.put("here.token.endpoint.url", tokenEndpointUrl);
        properties.put("here.access.keyid", accessKeyId);
        properties.put("here.access.key.secret", accessKeySecret);
        properties.put("here.token.scope", scope);

        fromProperties = new FromProperties(clock, properties);

        verifyFromProperties(true);
    }

    @Test(expected = NullPointerException.class) // OAuth1ClientCredentialsProvider complains
    public void test_properties_badsecret_constructor() {
        Properties properties = new Properties();

        properties.put("here.token.endpoint.url", tokenEndpointUrl);
        properties.put("here.access.key.id", accessKeyId);
        properties.put("here.access.keysecret", accessKeySecret);
        properties.put("here.token.scope", scope);

        fromProperties = new FromProperties(clock, properties);

        verifyFromProperties(true);
    }

    @Test(expected = AssertionError.class) // tokenEndpoint doesn't match
    public void test_properties_badurl_constructor() {
        Properties properties = new Properties();

        properties.put("here.token.endpointurl", tokenEndpointUrl);
        properties.put("here.access.key.id", accessKeyId);
        properties.put("here.access.key.secret", accessKeySecret);
        properties.put("here.token.scope", scope);

        fromProperties = new FromProperties(clock, properties);

        verifyFromProperties(true);
    }

    @Test(expected = AssertionError.class) // scope doesn't match
    public void test_properties_badscope_constructor() {
        Properties properties = new Properties();

        properties.put("here.token.endpoint.url", tokenEndpointUrl);
        properties.put("here.access.key.id", accessKeyId);
        properties.put("here.access.key.secret", accessKeySecret);
        properties.put("here.token.default.scope", scope);

        fromProperties = new FromProperties(clock, properties);

        verifyFromProperties(true);
    }
}
