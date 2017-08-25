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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
