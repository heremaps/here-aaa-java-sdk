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
package com.here.account.util;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.here.account.oauth2.AccessTokenResponse;

public class JsonSerializerTest {

    @Test
    public void test_pojo_extensible() throws JsonParseException, JsonMappingException, IOException {
        String arbitraryProperty = "prop"+UUID.randomUUID();
        String accessToken = "at"+UUID.randomUUID();
        String json = "{\"access_token\":\""+accessToken+"\",\"expires_in\":123,\""+arbitraryProperty+"\":\"asdf\"}";
        InputStream jsonInputStream = new ByteArrayInputStream(json.getBytes(JsonSerializer.CHARSET));
        AccessTokenResponse accessTokenResponse = JsonSerializer.toPojo(jsonInputStream, AccessTokenResponse.class);
        assertTrue("accessTokenResponse was null", null != accessTokenResponse);
        String actualAccessToken = accessTokenResponse.getAccessToken();
        assertTrue("expected accessToken "+accessToken+", actual "+actualAccessToken, accessToken.equals(actualAccessToken));
    }
    
    @Test
    public void test_escape_solidus() throws IOException {
        ObjectMapper MAPPER = new ObjectMapper();
        String solidusString = "{\"foo\":\"/\"}";
        String solidusStringEscaped = "{\"foo\":\"\\/\"}";
        Map<String, Object> map = (HashMap<String, Object>) MAPPER.readValue(new ByteArrayInputStream(
                solidusString.getBytes("UTF-8")), HashMap.class);
        String value = (String) map.get("foo");
        Map<String, Object> mapEscaped = (HashMap<String, Object>) MAPPER.readValue(new ByteArrayInputStream(
                solidusStringEscaped.getBytes("UTF-8")), HashMap.class);
        String valueEscaped = (String) map.get("foo");
        assertTrue("value "+value+" !.equals to escaped value "+valueEscaped, 
                value.equals(valueEscaped));
                
    }
}
