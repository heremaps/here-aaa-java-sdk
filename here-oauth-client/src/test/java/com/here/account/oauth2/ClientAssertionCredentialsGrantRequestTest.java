/*
 * Copyright (c) 2025 HERE Europe B.V.
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

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ClientAssertionCredentialsGrantRequestTest {

    private static final String FAKE_JWT = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0ZXN0In0.signature";

    @Test
    public void testFormParams_containsGrantType() {
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT);
        Map<String, List<String>> formParams = request.toFormParams();

        assertEquals("client_credentials", formParams.get("grant_type").get(0));
    }

    @Test
    public void testFormParams_containsClientAssertionType() {
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT);
        Map<String, List<String>> formParams = request.toFormParams();

        assertEquals("urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                formParams.get("client_assertion_type").get(0));
    }

    @Test
    public void testFormParams_containsClientAssertion() {
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT);
        Map<String, List<String>> formParams = request.toFormParams();

        assertEquals(FAKE_JWT, formParams.get("client_assertion").get(0));
    }

    @Test
    public void testFormParams_containsScope_whenSet() {
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT);
        request.setScope("openid");
        Map<String, List<String>> formParams = request.toFormParams();

        assertEquals("openid", formParams.get("scope").get(0));
    }

    @Test
    public void testFormParams_noScope_whenNotSet() {
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT);
        Map<String, List<String>> formParams = request.toFormParams();

        assertNull(formParams.get("scope"));
    }

    @Test
    public void testGetGrantType() {
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT);
        assertEquals("client_credentials", request.getGrantType());
    }

    @Test
    public void testGetClientAssertion() {
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT);
        assertEquals(FAKE_JWT, request.getClientAssertion());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_rejectsNull() {
        new ClientAssertionCredentialsGrantRequest(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_rejectsEmpty() {
        new ClientAssertionCredentialsGrantRequest("");
    }

    @Test
    public void testFormParams_singleResource() {
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT)
                .setResource(Collections.singletonList("https://example.com/api"));
        Map<String, List<String>> formParams = request.toFormParams();
        assertEquals(Collections.singletonList("https://example.com/api"), formParams.get("resource"));
    }

    @Test
    public void testFormParams_multipleResources() {
        List<String> resources = Arrays.asList("https://api.example.com", "https://data.example.com");
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT)
                .setResource(resources);
        Map<String, List<String>> formParams = request.toFormParams();
        assertEquals(resources, formParams.get("resource"));
    }

    @Test
    public void testFormParams_noResource() {
        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(FAKE_JWT);
        assertNull(request.toFormParams().get("resource"));
    }
}
