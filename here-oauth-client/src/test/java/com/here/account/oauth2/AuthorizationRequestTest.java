package com.here.account.oauth2;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

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
}
