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
import com.here.account.oauth2.bo.AccessTokenResponse;

public class JsonSerializerTest {

    @Test
    public void test_pojo_extensible() throws JsonParseException, JsonMappingException, IOException {
        String arbitraryProperty = "prop"+UUID.randomUUID();
        String accessToken = "at"+UUID.randomUUID();
        String json = "{\"accessToken\":\""+accessToken+"\",\"expiresIn\":123,\""+arbitraryProperty+"\":\"asdf\"}";
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
