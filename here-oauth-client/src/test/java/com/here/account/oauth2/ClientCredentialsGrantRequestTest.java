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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.here.account.util.JsonSerializer;
import com.here.account.util.OAuthConstants;

public class ClientCredentialsGrantRequestTest {

    @Test
    public void test_ClientCredentialsGrantRequest_json() {
        String scope = "scope";
        ClientCredentialsGrantRequest clientCredentialsGrantRequest = new ClientCredentialsGrantRequest();
        clientCredentialsGrantRequest.setScope(scope);
        String json = clientCredentialsGrantRequest.toJson();
        String expectedJson = "{\"grantType\":\"client_credentials\"," +
                "\"scope\":\"" + scope + "\"}";
        assertTrue("expected json "+expectedJson+", actual "+json, expectedJson.equals(json));
    }
    
    @Test
    public void test_ClientCredentialsGrantRequest_json_expiresIn() throws IOException {
        long expiresIn = 15;
        String scope = "scope";
        ClientCredentialsGrantRequest clientCredentialsGrantRequest = new ClientCredentialsGrantRequest().setExpiresIn(expiresIn);
        clientCredentialsGrantRequest.setScope(scope);
        String json = clientCredentialsGrantRequest.toJson();
        String expectedJson = "{\"grantType\":\"client_credentials\"," +
                "\"expiresIn\":"+expiresIn+", \"scope\":\"" + scope + "\"}";
        Map<String, Object> jsonMap = toMap(json);
        Map<String, Object> expectedMap = toMap(expectedJson);
        assertTrue("expected json "+expectedMap+", actual "+jsonMap, expectedMap.equals(jsonMap));
    }
    
    @Test
    public void test_ClientCredentialsGrantRequest_form() {
        ClientCredentialsGrantRequest clientCredentialsGrantRequest = new ClientCredentialsGrantRequest();
        Map<String, List<String>> form = clientCredentialsGrantRequest.toFormParams();
        Map<String, List<String>> expectedForm = new HashMap<String, List<String>>();
        expectedForm.put("grant_type", Collections.singletonList("client_credentials"));
        assertTrue("expected form "+expectedForm+", actual "+form, expectedForm.equals(form));
    }
    
    @Test
    public void test_ClientCredentialsGrantRequest_form_expiresIn() throws IOException {
        long expiresIn = 15;
        String scope = "test scope";
        ClientCredentialsGrantRequest clientCredentialsGrantRequest = new
                ClientCredentialsGrantRequest().setExpiresIn(expiresIn);
        clientCredentialsGrantRequest.setScope(scope);
        Map<String, List<String>> form = clientCredentialsGrantRequest.toFormParams();
        Map<String, List<String>> expectedForm = new HashMap<String, List<String>>();
        expectedForm.put("grant_type", Collections.singletonList("client_credentials"));
        expectedForm.put("expires_in", Collections.singletonList(""+expiresIn));
        expectedForm.put("scope", Collections.singletonList(scope));
        assertTrue("expected form "+expectedForm+", actual "+form, expectedForm.equals(form));
    }


    @Test
    public void test_ClientCredentialsGrantRequest_form_singleResource() {
        ClientCredentialsGrantRequest request = new ClientCredentialsGrantRequest()
                .setResource(Collections.singletonList("https://example.com/api"));
        Map<String, List<String>> form = request.toFormParams();
        assertEquals(Collections.singletonList("https://example.com/api"), form.get("resource"));
    }

    @Test
    public void test_ClientCredentialsGrantRequest_form_multipleResources() {
        List<String> resources = Arrays.asList("https://api.example.com", "https://data.example.com");
        ClientCredentialsGrantRequest request = new ClientCredentialsGrantRequest()
                .setResource(resources);
        Map<String, List<String>> form = request.toFormParams();
        assertEquals(resources, form.get("resource"));
    }

    @Test
    public void test_ClientCredentialsGrantRequest_form_noResource() {
        ClientCredentialsGrantRequest request = new ClientCredentialsGrantRequest();
        Map<String, List<String>> form = request.toFormParams();
        assertNull(form.get("resource"));
    }

    @Test
    public void test_ClientCredentialsGrantRequest_getResource() {
        List<String> resources = Collections.singletonList("https://example.com");
        ClientCredentialsGrantRequest request = new ClientCredentialsGrantRequest()
                .setResource(resources);
        assertEquals(resources, request.getResource());
    }

    @Test
    public void test_addFormParams_nullFormParams_noOp() {
        // must not throw
        AccessTokenRequest.addFormParams(null, "resource", Collections.singletonList("https://example.com"));
    }

    @Test
    public void test_addFormParams_nullName_noOp() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        AccessTokenRequest.addFormParams(formParams, null, Collections.singletonList("https://example.com"));
        assertTrue(formParams.isEmpty());
    }

    @Test
    public void test_addFormParams_nullValues_noOp() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        AccessTokenRequest.addFormParams(formParams, "resource", null);
        assertTrue(formParams.isEmpty());
    }

    @Test
    public void test_addFormParams_emptyValues_noOp() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        AccessTokenRequest.addFormParams(formParams, "resource", Collections.<String>emptyList());
        assertTrue(formParams.isEmpty());
    }

    @Test
    public void test_addFormParams_defensiveCopy() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        List<String> values = new java.util.ArrayList<String>(Collections.singletonList("https://example.com"));
        AccessTokenRequest.addFormParams(formParams, "resource", values);
        values.add("https://other.com");
        assertEquals(1, formParams.get("resource").size());
    }


    private Map<String, Object> toMap(String json) throws IOException {
        byte[] bytes = json.getBytes(OAuthConstants.UTF_8_CHARSET);
        ByteArrayInputStream jsonInputStream = null;
        try {
            jsonInputStream = new ByteArrayInputStream(bytes);
            return JsonSerializer.toMap(jsonInputStream);
        } finally {
            if (null != jsonInputStream) {
                jsonInputStream.close();
            }
        }
    }

}
