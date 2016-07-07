package com.here.account.auth;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;

import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.util.Clock;

public class OAuth1SignerTest {

    private String clientId = "clientId";
    private String clientSecret = "clientSecret";
    
    private OAuth1Signer oauth1Signer;
    
    MyHttpRequest httpRequest;
    
    private static class MyHttpRequest implements HttpRequest {
        
        private String authorizationHeader;
        
        public MyHttpRequest() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addAuthorizationHeader(String value) {
            this.authorizationHeader = value;
        }

        public String getAuthorizationHeader() {
            return this.authorizationHeader;
        }
        
    }
    
    private String method;
    private String url;
    private Clock clock;
    
    @Before
    public void setUp() {
        this.clock = new Clock() {

            @Override
            public long currentTimeMillis() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void schedule(ScheduledExecutorService scheduledExecutorService, Runnable runnable,
                    long millisecondsInTheFutureToSchedule) {
                // TODO Auto-generated method stub
                
            }
            
        };
        this.oauth1Signer = new MyOAuth1Signer(clock, clientId, clientSecret);
        this.httpRequest = new MyHttpRequest();
        this.method = "GET";
        this.url = "http://localhost:8080/whatever";
    }
    
    protected static class MyOAuth1Signer extends OAuth1Signer {
        public MyOAuth1Signer(Clock clock, String clientId, String clientSecret) {
            super(clock, clientId, clientSecret);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void nextBytes(byte[] bytes) {
            byte a = 0x00;
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = a;
                a += 0x01;
            }
        }
    }
    
    @Test
    public void test_sign_formParams_impactsSignature() {
        Map<String, List<String>> formParams = null;
        
        oauth1Signer.authorize(httpRequest, method, url, formParams);
        String signatureNull = httpRequest.getAuthorizationHeader();
        
        formParams = new HashMap<String, List<String>>();
        oauth1Signer.authorize(httpRequest, method, url, formParams);
        String signatureEmpty = httpRequest.getAuthorizationHeader();
        
        formParams.put("foo", Collections.singletonList("bar"));
        oauth1Signer.authorize(httpRequest, method, url, formParams);
        String signatureFooBar = httpRequest.getAuthorizationHeader();

        formParams.put("foo", Collections.singletonList("none"));
        oauth1Signer.authorize(httpRequest, method, url, formParams);
        String signatureFooNone = httpRequest.getAuthorizationHeader();

        assertTrue("signatureNull was null, but shouldn't be", null != signatureNull);
        assertTrue("signatureEmpty was null, but shouldn't be", null != signatureEmpty);
        assertTrue("signatureFooBar was null, but shouldn't be", null != signatureFooBar);
        assertTrue("signatureFooNone was null, but shouldn't be", null != signatureFooNone);
        
        assertTrue("signatureNull "+signatureNull+" doesn't match signatureEmpty "+signatureEmpty,
                signatureNull.equals(signatureEmpty));
        assertTrue("signatureNull "+signatureNull+" matches signatureFooBar "+signatureFooBar+", but shouldn't",
                !signatureNull.equals(signatureFooBar));
        assertTrue("signatureFooBar "+signatureFooBar+" matches signatureFooNone "+signatureFooNone+", but shouldn't",
                !signatureFooBar.equals(signatureFooNone));
    }
}
