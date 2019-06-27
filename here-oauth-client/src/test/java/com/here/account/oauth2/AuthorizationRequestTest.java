package com.here.account.oauth2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.here.account.util.JacksonSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;

import static org.junit.Assert.*;

public class AuthorizationRequestTest {
    
    AccessTokenRequest authorizationRequest;
    
    @Before
    public void setUp() {
        authorizationRequest = new AccessTokenRequest("foo") {

            @Override
            public String toJson() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Map<String, List<String>> toFormParams() {
                // TODO Auto-generated method stub
                return null;
            }
            
        };

    }

    @Test
    public void test_addFormParam_null() {
        
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        String name = "name";
        Object value = null;
        authorizationRequest.addFormParam(formParams, name, value);
        
        assertTrue("formParams was expected empty, but wasn't", formParams.isEmpty());
    }
    
    @Test
    public void addFormParam() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        String name = "name";
        Object value = "value";
        authorizationRequest.addFormParam(formParams, name, value);
        
        int size = formParams.size();
        int expectedSize = 1;
        
        assertTrue("formParams size was expected "+expectedSize+", actual "+size, size == expectedSize);
        List<String> valueList = formParams.get(name);
        size = valueList.size();
        assertTrue("valueList size was expected "+expectedSize+", actual "+size, size == expectedSize);

        String firstValue = valueList.get(0);
        assertTrue("firstValue was expected "+value+", actual "+firstValue, value.equals(firstValue));
    }

    @Test
    public void test_request_body() {
        Long expiresIn = 123456789L;
        String scope = "testScope";
        String correlationId = "corrId_abc123";

        Map<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.put("testKey", "testValue");

        JacksonSerializer serializer = new JacksonSerializer();

        authorizationRequest.setExpiresIn(expiresIn);
        authorizationRequest.setScope(scope);
        authorizationRequest.setAdditionalHeaders(additionalHeaders);
        authorizationRequest.setCorrelationId(correlationId);

        String json = serializer.objectToJson(authorizationRequest);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode body = mapper.readTree(json);

            Long actualExpiresIn = body.get("expiresIn").asLong();
            assertEquals("expiresIn expected "+expiresIn+", actual "+actualExpiresIn, expiresIn, actualExpiresIn);

            String actualScope = body.get("scope").asText();
            assertEquals("scope expected "+scope+", actual "+actualScope, scope, actualScope);

            // AccessTokenRequest transient values should not get serialized
            assertFalse("additionalHeaders was expected to not exist, but it does", body.has("additionalHeaders"));
            assertFalse("correlationId was expected to not exist, but it does", body.has("correlationId"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
