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
package com.here.account.http.java;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.here.account.auth.NoAuthorizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.here.account.auth.OAuth2Authorizer;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.http.HttpProvider.HttpResponse;
import com.here.account.util.JsonSerializer;

public class JavaHttpProviderTest {
    
    private final String urlString = "http://www.example.com/";
    private String method = "GET";
    private Map<String, List<String>> formParams;
    private String jsonBody;
    boolean callGetRequest = false;

    @Before
    public void setUp() throws IOException {
    }
    
    @Test
    public void test_example() throws HttpException, IOException {
        doRequest();
    }
    
    @Test
    public void test_example_callGetRequest() throws HttpException, IOException {
        callGetRequest = true;
        jsonBody = "foo";
        
        doRequest();
    }

    
    String expectedContentType;
    
    @Test
    public void test_post_formBody() throws HttpException, IOException {
        method = "POST";
        formParams = new HashMap<String, List<String>>();
        formParams.put("foo", Collections.singletonList("bar"));
        doRequest();
        byte[] body = byteArrayOutputStream.toByteArray();
        byte[] expectedBody = "foo=bar".getBytes(JsonSerializer.CHARSET);
        verifyBytes(body, expectedBody);

        expectedContentType = "application/x-www-form-urlencoded";
        verifyContentType();
    }
    
    private void verifyContentType() {
        String actualContentType = requestHeaders.get("Content-Type");
        assertTrue("expectedContentType "+expectedContentType+" !.equal to actualContentType "+actualContentType, 
                expectedContentType.equals(actualContentType));
    }
    
    @Test
    public void test_put_json() throws HttpException, IOException {
        method = "PUT";
        jsonBody = "{\"foo\":\"bar\"}";
        doRequest();
        byte[] body = byteArrayOutputStream.toByteArray();
        byte[] expectedBody = jsonBody.getBytes(JsonSerializer.CHARSET);
        verifyBytes(body, expectedBody);
        
        expectedContentType = "application/json";
        verifyContentType();

    }
    
    protected boolean bytesMatch(byte[] actualBody, byte[] expectedBody) {
        try {
            verifyBytes(actualBody, expectedBody);
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }
    
    protected void verifyBytes(byte[] actualBody, byte[] expectedBody) {
        assertTrue("nullness didn't match", !(null != actualBody ^ null != expectedBody));
        if (null != actualBody) {
            int actualLength = actualBody.length;
            int expectedLength = expectedBody.length;
            assertTrue("expected length "+expectedLength+", actual length "+actualLength, expectedLength == actualLength);
            for (int i = 0; i < actualLength; i++) {
                byte actualByte = actualBody[i];
                byte expectedByte = expectedBody[i];
                assertTrue("expected byte "+expectedByte+", actual byte "+actualByte, expectedByte == actualByte);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class) 
    public void test_wrongRequestClass() throws HttpException, IOException {
        JavaHttpProvider javaHttpProvider = (JavaHttpProvider) JavaHttpProvider.builder().build();
        HttpRequest httpRequest = new HttpRequest() {

            @Override
            public void addAuthorizationHeader(String value) {
                // no-op
            }
            
        };
        javaHttpProvider.execute(httpRequest);
    }
    
    HttpRequestAuthorizer httpRequestAuthorizer = new OAuth2Authorizer("my-accessToken");

    protected void doRequest() throws MalformedURLException, IOException, HttpException {
        JavaHttpProvider javaHttpProvider = (JavaHttpProvider) JavaHttpProvider.builder().build();
        JavaHttpProvider mock = Mockito.spy(javaHttpProvider);
        Mockito.doReturn(getMockHttpUrlConnection()).when(mock).getHttpUrlConnection(Mockito.anyString());

        HttpProvider.HttpRequest httpRequest;
        if (callGetRequest) {
            httpRequest = mock.getRequest(httpRequestAuthorizer, method, urlString, jsonBody);
        } else {
            if (null != jsonBody) {
                httpRequest = javaHttpProvider.getRequest(httpRequestAuthorizer, method, urlString, jsonBody);
            } else {
                httpRequest = javaHttpProvider.getRequest(httpRequestAuthorizer, method, urlString, formParams);
            }
        }

        HttpResponse httpResponse = mock.execute(httpRequest);
        assertTrue("httpResponse was null", null != httpResponse);
    }
    
    @Test
    public void test_getFormBody_keyOnly() throws UnsupportedEncodingException {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        List<String> valueList= new ArrayList<String>();
        formParams.put("ant", valueList);
        formParams.put("bar", null);

        byte[] formBody = JavaHttpProvider.getFormBody(formParams);
        String expectedOption1 = "ant&bar";
        String expectedOption2 = "bar&ant";
        byte[] expectedBodyOption1 = expectedOption1.getBytes("UTF-8");
        byte[] expectedBodyOption2 = expectedOption2.getBytes("UTF-8");
        assertTrue("formBody "+formBody+" didn't match one of expected "+expectedBodyOption1+" or "+expectedBodyOption2, 
                bytesMatch(formBody, expectedBodyOption1) || bytesMatch(formBody, expectedBodyOption2));
    }
    
    @Test
    public void test_getFormBody_second() throws UnsupportedEncodingException {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        List<String> valueList= Collections.singletonList("bar");
        formParams.put("foo", valueList);
        List<String> valueList2 = Collections.singletonList("dog");
        formParams.put("ant", valueList2);
        byte[] formBody = JavaHttpProvider.getFormBody(formParams);
        String expectedOption1 = "foo=bar&ant=dog";
        String expectedOption2 = "ant=dog&foo=bar";
        byte[] expectedBodyOption1 = expectedOption1.getBytes("UTF-8");
        byte[] expectedBodyOption2 = expectedOption2.getBytes("UTF-8");
        assertTrue("formBody "+formBody+" didn't match one of expected "+expectedBodyOption1+" or "+expectedBodyOption2, 
                bytesMatch(formBody, expectedBodyOption1) || bytesMatch(formBody, expectedBodyOption2));
    }
    
    @Test
    public void test_getFormBody_second2() throws UnsupportedEncodingException {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        List<String> valueList= Collections.singletonList("bar");
        formParams.put("foo", valueList);
        List<String> valueList2 = new ArrayList<String>();
        valueList2.add("dog");
        valueList2.add("cat");
        formParams.put("ant", valueList2);
        byte[] formBody = JavaHttpProvider.getFormBody(formParams);
        String expectedOption1 = "foo=bar&ant=dog&ant=cat";
        String expectedOption2 = "foo=bar&ant=cat&ant=dog";
        String expectedOption3 = "ant=dog&foo=bar&ant=cat";
        String expectedOption4 = "ant=dog&ant=cat&foo=bar";
        String expectedOption5 = "ant=cat&foo=bar&ant=dog";
        String expectedOption6 = "ant=cat&ant=dog&foo=bar";
        assertTrue("formBody "+formBody+" didn't match one of expected",
                bytesMatch(formBody, expectedOption1.getBytes("UTF-8")) 
                || bytesMatch(formBody, expectedOption2.getBytes("UTF-8"))
                || bytesMatch(formBody, expectedOption3.getBytes("UTF-8"))
                || bytesMatch(formBody, expectedOption4.getBytes("UTF-8"))
                || bytesMatch(formBody, expectedOption5.getBytes("UTF-8"))
                || bytesMatch(formBody, expectedOption6.getBytes("UTF-8"))
                );
    }

    
    @Test
    public void test_404() throws HttpException, IOException {
        statusCode = 404;
        doRequest();
    }
    
    @Test
    public void test_authorizationHeader() throws HttpException, IOException {
        doRequest();
    }
    @Test
    public void test_put() throws HttpException, IOException {
        method = "PUT";
        doRequest();
    }
    
    @Test
    public void test_openConnection() throws IOException {
        JavaHttpProvider javaHttpProvider = (JavaHttpProvider) JavaHttpProvider.builder().build();
        String urlString = "http://localhost:8080/foo";
        HttpURLConnection httpUrlConnection = javaHttpProvider.getHttpUrlConnection(urlString);
        // not actually connected, unless you call .open
        assertTrue("httpUrlConnection was null", null != httpUrlConnection);
    }
    
    @Test
    public void test_noAuthorizationHeader() throws IOException, HttpException {
        httpRequestAuthorizer = new NoAuthorizer();

        doRequest();
    }



    int statusCode = 200;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Map<String, String> requestHeaders = new HashMap<String, String>();

    private HttpURLConnection getMockHttpUrlConnection() throws MalformedURLException {
        URL u = new URL(urlString);
        return new HttpURLConnection( u) {
            
            @Override
            public int getResponseCode() {
                return statusCode;
            }

            @Override
            public void disconnect() {
                // TODO Auto-generated method stub
                
            }

            @Override
            public boolean usingProxy() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void connect() throws IOException {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void setRequestProperty(String headerName, String headerValue) {
                super.setRequestProperty(headerName, headerValue);
                requestHeaders.put(headerName, headerValue);
            }

            @Override
            public String getHeaderField(String headerFieldName) {
                if (HttpConstants.CONTENT_LENGTH_HEADER.equals(headerFieldName)) {
                    return "" + urlString.getBytes(JsonSerializer.CHARSET).length;
                }
                return null;
            }
            
            @Override
            public OutputStream getOutputStream() throws IOException {
                // wrap the output capturing stream in this.
                return new OutputStream() {

                    @Override
                    public void write(int b) throws IOException {
                        byteArrayOutputStream.write(b);
                    }
                    
                };
            }
            
            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(urlString.getBytes(JsonSerializer.CHARSET));
            }
            
        };
    }

}
