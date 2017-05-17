package com.here.account.http.java;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider.HttpResponse;
import com.here.account.util.JsonSerializer;

public class JavaHttpProviderTest {
    
    private final String urlString = "http://www.example.com/";
    private String method = "GET";
    private String authorizationHeader;
    private Map<String, List<String>> formParams;
    private String jsonBody;

    @Before
    public void setUp() throws IOException {
    }
    
    @Test
    public void test_example() throws HttpException, IOException {
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

    protected void doRequest() throws MalformedURLException, IOException, HttpException {
        JavaHttpProvider javaHttpProvider = (JavaHttpProvider) JavaHttpProvider.builder().build();
        JavaHttpProvider mock = Mockito.spy(javaHttpProvider);
        Mockito.doReturn(getMockHttpUrlConnection()).when(mock).getHttpUrlConnection(Mockito.anyString());

        JavaHttpProvider.JavaHttpRequest httpRequest = new JavaHttpProvider.JavaHttpRequest(method, urlString, jsonBody, formParams);
        if (null != authorizationHeader) {
            httpRequest.addAuthorizationHeader(authorizationHeader);
        }

        HttpResponse httpResponse = mock.execute(httpRequest);
        assertTrue("httpResponse was null", null != httpResponse);
    }
    
    @Test
    public void test_404() throws HttpException, IOException {
        statusCode = 404;
        doRequest();
    }
    
    @Test
    public void test_authorizationHeader() throws HttpException, IOException {
        authorizationHeader = "Bearer my-token";
        doRequest();
    }
    @Test
    public void test_put() throws HttpException, IOException {
        method = "PUT";
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
