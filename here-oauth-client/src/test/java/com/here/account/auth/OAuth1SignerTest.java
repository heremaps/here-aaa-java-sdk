/*
 * Copyright 2016 HERE Global B.V.
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
package com.here.account.auth;

import com.here.account.auth.OAuth1Signer;
import static org.junit.Assert.assertTrue;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
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
    
    /**
     * Demonstrate the tradeoffs between HmacSHA1 and HmacSHA256 signature methods.
     * OAuth1 signature spec calls out the former by name, 
     * but is extensible to the latter as well.
     * 
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    @Test
    public void test_HmacSHA1_HmacSHA256() throws NoSuchAlgorithmException, InvalidKeyException {
        String key = UUID.randomUUID().toString();
        
        String input = "my dog has fleas";
        
        String sig1 = HmacSHAN(key, "HmacSHA1", input);
        assertTrue("sig1 was null for HmacSHA1", null != sig1);
        
        String sig256 = HmacSHAN(key, "HmacSHA256", input);
        assertTrue("sig256 was null for HmacSHA256", null != sig256);
        
        assertTrue("sig1 "+sig1+" wasn't smaller than sig256 "+sig256, sig1.length() < sig256.length());

    }
    
    protected String HmacSHAN(String keyString, String algorithm, String baseString) throws NoSuchAlgorithmException, InvalidKeyException {
        Key signingKey = new SecretKeySpec(keyString.getBytes(), algorithm);
        Mac mac = Mac.getInstance(algorithm);
        mac.init(signingKey);
    
        //generate signature bytes
        byte[] signatureBytes = mac.doFinal(baseString.toString().getBytes());
    
        // base64-encode the hmac
        return new Base64().encodeAsString(signatureBytes);
    }


}
