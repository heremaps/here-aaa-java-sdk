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
package com.here.account.http.apache;

import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ApacheHttpClientProviderTest {
    
    HttpRequestAuthorizer httpRequestAuthorizer;
    
    @Before
    public void setUp() {
        httpRequestAuthorizer = mock(HttpRequestAuthorizer.class);
        doAnswer(new Answer<Void>() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            //headers.put((String)invocation.getArguments()[0], (String)invocation.getArguments()[1]);
            return null;
          }
        }).when(httpRequestAuthorizer).authorize(any(HttpRequest.class), any(String.class), any(String.class), 
                (Map<String, List<String>>) any(Map.class));

        httpProvider = ApacheHttpClientProvider.builder().build();
        url = "http://localhost:8080/path/to";
        formParams = null;
    }
    
    @Test
    public void test_javadocs() throws IOException {
        
        HttpProvider httpProvider = ApacheHttpClientProvider.builder().build();
        // use httpProvider such as with HereAccessTokenProviders...
        
        assertTrue("httpProvider was null", null != httpProvider);
        httpProvider.close();
    }
    
    HttpRequest httpRequest;
    HttpProvider httpProvider;
    String url;
    Map<String, List<String>> formParams;
    
    @Test(expected = IllegalArgumentException.class) 
    public void test_wrongRequestClass() throws HttpException, IOException {
        httpProvider = (ApacheHttpClientProvider) ApacheHttpClientProvider.builder().build();
        HttpRequest httpRequest = new HttpRequest() {

            @Override
            public void addAuthorizationHeader(String value) {
                // no-op
            }
            
        };
        httpProvider.execute(httpRequest);
    }

    @Test
    public void test_ApacheHttpClientResponse() throws HttpException, IOException {
        String requestBodyJson = "{\"foo\":\"bar\"}";
        url = "http://google.com";

        httpProvider = ApacheHttpClientProvider.builder().build();
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "PUT", url, requestBodyJson);
        HttpProvider.HttpResponse response = httpProvider.execute(httpRequest);
        assertEquals(HttpURLConnection.HTTP_BAD_METHOD, response.getStatusCode());
        assertNotNull("response body is null", response.getResponseBody());
        assertTrue("response content length is 0", 0<response.getContentLength());
    }

    @Test
    public void test_badUri() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        url = "htp:/ asdf8080:a:z";
        try {
            httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "GET", url, formParams);
            fail("should have thrown exception for url "+url+", but didn't");
        } catch (IllegalArgumentException e) {
            // expected
            String message = e.getMessage();
            String expectedContains = "malformed URL";
            assertTrue("expected contains "+expectedContains+", actual "+message, message.contains(expectedContains));
        }
    }
    
    @Test
    public void test_methodDoesntSupportFormParams() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        formParams = new HashMap<String, List<String>>();
        formParams.put("foo", Collections.singletonList("bar"));
        try {
            httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "DELETE", url, formParams);
            fail("should have thrown exception for formParams with method DELETE, but didn't");
        } catch (IllegalArgumentException e) {
            // expected
            String message = e.getMessage();
            String expectedContains = "no formParams permitted for method";
            assertTrue("expected contains "+expectedContains+", actual "+message, message.contains(expectedContains));
        }
    }
    
    @Test
    public void test_formParamsPut() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        formParams = new HashMap<String, List<String>>();
        formParams.put("foo", Collections.singletonList("bar"));
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "PUT", url, formParams);
        HttpRequestBase httpRequestBase = getHttpRequestBase();
        HttpPut httpPut = (HttpPut) httpRequestBase;
        HttpEntity httpEntity = httpPut.getEntity();
        assertTrue("httpEntity was null", null != httpEntity);
    }

    @Test
    public void test_formParamsPut_null() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        formParams = null;
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "PUT", url, formParams);
        HttpRequestBase httpRequestBase = getHttpRequestBase();
        HttpPut httpPut = (HttpPut) httpRequestBase;
        HttpEntity httpEntity = httpPut.getEntity();
        assertTrue("httpEntity was expected null, actual "+httpEntity, null == httpEntity);
    }

    
    @Test
    public void test_methodDoesntSupportJson() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        String requestBodyJson = "{\"foo\":\"bar\"}";
        try {
            httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "DELETE", url, requestBodyJson);
            fail("should have thrown exception for JSON body with method DELETE, but didn't");
        } catch (IllegalArgumentException e) {
            // expected
            String message = e.getMessage();
            String expectedContains = "no JSON request body permitted for method";
            assertTrue("expected contains "+expectedContains+", actual "+message, message.contains(expectedContains));
        }
    }

    
    @Test
    public void test_jsonPut() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        String requestBodyJson = "{\"foo\":\"bar\"}";
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "PUT", url, requestBodyJson);
        HttpRequestBase httpRequestBase = getHttpRequestBase();
        HttpPut httpPut = (HttpPut) httpRequestBase;
        HttpEntity httpEntity = httpPut.getEntity();
        assertTrue("httpEntity was null", null != httpEntity);
    }
    
    @Test
    public void test_jsonPut_null() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        String requestBodyJson = null;
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "PUT", url, requestBodyJson);
        HttpRequestBase httpRequestBase = getHttpRequestBase();
        HttpPut httpPut = (HttpPut) httpRequestBase;
        HttpEntity httpEntity = httpPut.getEntity();
        assertTrue("httpEntity was expected null, but was "+httpEntity, null == httpEntity);
    }

    @Test
    public void test_methods() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
        verifyApacheType("GET", HttpGet.class);
        verifyApacheType("POST", HttpPost.class);
        verifyApacheType("PUT", HttpPut.class);
        verifyApacheType("DELETE", HttpDelete.class);
        verifyApacheType("HEAD", HttpHead.class);
        verifyApacheType("OPTIONS", HttpOptions.class);
        verifyApacheType("TRACE", HttpTrace.class);
        verifyApacheType("PATCH", HttpPatch.class);
        try {
            verifyApacheType("BROKENMETHOD", null);
            fail("BROKENMETHOD should have thrown IllegalArgumentException, but didn't");
        } catch (IllegalArgumentException e) {
            // expected
            String message = e.getMessage();
            String expectedContains = "no support for request method=BROKENMETHOD";
            assertTrue("expected contains "+expectedContains+", actual "+message, message.contains(expectedContains));
        }
    }
    
    @Test
    public void test_assignHttpClientDirectly() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        CloseableHttpClient mock = mock(CloseableHttpClient.class);
        ApacheHttpClientProvider provider = (ApacheHttpClientProvider)ApacheHttpClientProvider.builder().setHttpClient(mock).build();
        CloseableHttpClient fromProvider = extractHttpClient(provider);
        assertTrue("client must be SAME object",mock==fromProvider);
    }
    
    @Test
    public void test_setDoCloseToFalse() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
        CloseableHttpClient mock = mock(CloseableHttpClient.class);
        ApacheHttpClientProvider provider = (ApacheHttpClientProvider)ApacheHttpClientProvider.builder()
                .setHttpClient(mock)
                .setDoCloseHttpClient(false).build();

        provider.close();
        verify(mock,times(0)).close();
    }
    
    @Test
    public void test_setDoCloseToTrue() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
        CloseableHttpClient mock = mock(CloseableHttpClient.class);
        ApacheHttpClientProvider provider = (ApacheHttpClientProvider)ApacheHttpClientProvider.builder()
                .setHttpClient(mock).build();
        
        provider.close();
        verify(mock,times(1)).close();
    }
    
    protected static CloseableHttpClient extractHttpClient(ApacheHttpClientProvider provider)  throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field httpClientField = ApacheHttpClientProvider.class.getDeclaredField("httpClient");
        assertTrue("field was null", null != httpClientField);
        httpClientField.setAccessible(true);
        Object o = httpClientField.get(provider);
        assertTrue("o was null", null != o);
        assertTrue("o wasn't an HttpRequestBase", CloseableHttpClient.class.isAssignableFrom(o.getClass()));

        return (CloseableHttpClient) o;
        
    }
    
    protected void verifyApacheType(String method, Class<?> clazz) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, method, url, formParams);

        Class<?> expectedType = Class.forName("com.here.account.http.apache.ApacheHttpClientProvider$ApacheHttpClientRequest");
        Class<?> actualType = httpRequest.getClass();
        assertTrue("httpRequest was not expected "+expectedType+", actual "+actualType, expectedType.equals(actualType));
        
        HttpRequestBase o = getHttpRequestBase();
        expectedType = clazz;
        actualType = o.getClass();
        assertTrue("o was wrong type, expected "+expectedType+", actual "+actualType, expectedType.equals(actualType));
    }
    
    protected HttpRequestBase getHttpRequestBase() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Class<?> actualType = httpRequest.getClass();
        Field field = actualType.getDeclaredField("httpRequestBase");
        assertTrue("field was null", null != field);
        field.setAccessible(true);
        Object o = field.get(httpRequest);
        assertTrue("o was null", null != o);
        assertTrue("o wasn't an HttpRequestBase", HttpRequestBase.class.isAssignableFrom(o.getClass()));

        return (HttpRequestBase) o;
    }
}
