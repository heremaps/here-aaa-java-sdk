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
package com.here.account.auth;

import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.util.Clock;
import com.here.account.util.OAuthConstants;

public class OAuth1SignerTest {

    private String accessKeyId = "access-key-id";
    private String accessKeySecret = "access-key-secret";
    
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
    
    private long clockCurrentTimeMillis = 0;
    
    @Before
    public void setUp() {
        this.clock = new Clock() {

            @Override
            public long currentTimeMillis() {
                // TODO Auto-generated method stub
                return clockCurrentTimeMillis;
            }

            @Override
            public void schedule(ScheduledExecutorService scheduledExecutorService, Runnable runnable,
                    long millisecondsInTheFutureToSchedule) {
                // TODO Auto-generated method stub
                
            }
            
        };
        this.oauth1Signer = new MyOAuth1Signer(clock, accessKeyId, accessKeySecret);
        this.httpRequest = new MyHttpRequest();
        this.method = "GET";
        this.url = "http://localhost:8080/whatever";
    }
    
    protected static class MyOAuth1Signer extends OAuth1Signer {
        public MyOAuth1Signer(Clock clock, String accessKeyId, String accessKeySecret) {
            super(clock, accessKeyId, accessKeySecret);
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
     * @throws UnsupportedEncodingException 
     */
    @Test
    public void test_HmacSHA1_HmacSHA256() throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String key = UUID.randomUUID().toString();
        
        String input = "my dog has fleas";
        
        String sig1 = HmacSHAN(key, "HmacSHA1", input);
        assertTrue("sig1 was null for HmacSHA1", null != sig1);
        
        String sig256 = HmacSHAN(key, "HmacSHA256", input);
        assertTrue("sig256 was null for HmacSHA256", null != sig256);
        
        assertTrue("sig1 "+sig1+" wasn't smaller than sig256 "+sig256, sig1.length() < sig256.length());
    }
    
    @Test
    public void test_OAuth1Signer_uses_HMAC_SHA256() throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        this.clockCurrentTimeMillis = System.currentTimeMillis();
        
        byte[] nonceBytes = {
                0x00,
                0x01,
                0x02,
                0x03,
                0x04,
                0x05
        };
        String nonce = Base64.encodeBase64URLSafeString(nonceBytes).substring(0, nonceBytes.length);
        
        long oauth_timestamp = clockCurrentTimeMillis / 1000L; // oauth1 uses seconds

        String shaVariant = "SHA256";//"SHA1";
        
        String requestParameters = "oauth_consumer_key="+accessKeyId+"&oauth_nonce="+nonce+"&oauth_signature_method=HMAC-"+shaVariant+"&oauth_timestamp="+oauth_timestamp+"&oauth_version=1.0";
        String signatureBaseString = "GET&"
                +urlEncode(url)+"&"
                + urlEncode(requestParameters); // no request parameters in this test case
        

        System.out.println("test       signatureBaseString "+signatureBaseString);
        
        String key = urlEncode(accessKeySecret) + "&"; // no token shared-secret
        
        String expectedSignature = HmacSHAN(key, "Hmac"+shaVariant, signatureBaseString);

        String expectedSignatureInAuthorizationHeader = urlEncode(expectedSignature);
        
        oauth1Signer.authorize(httpRequest, method, url, null);
        String actualHeader = httpRequest.getAuthorizationHeader();
        
        Pattern pattern = Pattern.compile("\\A.*oauth_signature=\\\"([^\\\"]+).*\\z");
        Matcher matcher = pattern.matcher(actualHeader);
        assertTrue("pattern wasn't matched: "+actualHeader, matcher.matches());
        String actualSignature = matcher.group(1);
        
        
        assertTrue("expected signature "+expectedSignatureInAuthorizationHeader+", actual signature "+actualSignature, 
                expectedSignatureInAuthorizationHeader.equals(actualSignature));
    }
    
    private String urlEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, OAuthConstants.UTF_8_STRING).replaceAll("\\+", "%20");
    }

    
    protected String HmacSHAN(String keyString, String algorithm, String baseString) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        /*
                     byte[] keyBytes = (urlEncode(consumerSecret) + "&").getBytes(OAuthConstants.UTF_8_CHARSET);
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, signatureMethod);

            //generate signature based on the requested signature method
            Mac mac = Mac.getInstance(signatureMethod);
            mac.init(signingKey);
            byte[] signedBytes = mac.doFinal(bytesToSign);
            return Base64.encodeBase64String(signedBytes);

         */
        byte[] keyBytes = keyString.getBytes("UTF-8");
        Key signingKey = new SecretKeySpec(keyBytes, algorithm);
        Mac mac = Mac.getInstance(algorithm);
        mac.init(signingKey);
    
        //generate signature bytes
        byte[] signatureBytes = mac.doFinal(baseString.getBytes("UTF-8"));
    
        // base64-encode the hmac
        //return new Base64().encodeAsString(signatureBytes);
        return Base64.encodeBase64String(signatureBytes);
    }
}
