/*
 * Copyright (c) 2018 HERE Europe B.V.
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

import org.asynchttpclient.Param;
import org.asynchttpclient.oauth.ConsumerKey;
import org.asynchttpclient.oauth.OAuthSignatureCalculatorInstance;
import org.asynchttpclient.oauth.RequestToken;
import org.asynchttpclient.uri.Uri;
import org.asynchttpclient.util.Utf8UrlEncoder;
import org.junit.Test;

import java.lang.reflect.Method;
import java.security.*;
import java.security.spec.*;
import java.util.*;

import static com.here.account.auth.SignatureCalculator.ELLIPTIC_CURVE_ALGORITHM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SignatureCalculatorTest {
    static final private String consumerKey = "testKey";
    static final private String consumerSecret = "testSecret";
    static final private String nonce = "ab1Xo3";
    static final private Long timestamp = 123456789L;
    static final private String method = "POST";
    static final private String baseURL = "https://www.sampleurl.com/some/path";
    static final private String baseURLWithPort = "https://www.sampleurl.com:443/some/path";
    static final private String baseURLWithNonStandardPort = "https://www.sampleurl.com:9000/some/path";
    static final private String baseURLWithPort80 = "http://www.sampleurl.com:80/some/path";
    static final private Map<String, List<String>> params = createParamsList();

    //expected signatures from SHA256 - obtained using the same parameters via postman.
    static final private String simpleSha256 = "FeKRSQawLAt3GwVBYCJqY2lawFkEHs2u78MfT7m1P+A=";
    static final private String withFormParamSha256 = "THFhaWB1Lbp/SVU+FSc/ix4CS6LpNY1suaZBSKQ4na4=";
    static final private String withFormAndQueryParamSha256 = "bw5G7oIjsicO2Lp8rNa1H0bNj3Pkv+5aP2g9EedRpxI=";

    /////////////////////////////// HMAC-SHA1 //////////////////////////////////////////
    @Test
    public void testSignatureHmacSha1() throws Exception {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURL, null, null);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA1, null, null);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithFormParams() throws Exception {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURL, params, null);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA1, params, null);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithFormParamsWithSpacesInValue() throws Exception {

        Map<String, List<String>> nestedParams = new HashMap<>();
        nestedParams.put("http_method", Arrays.asList("POST"));
        nestedParams.put("http_u_r_l", Arrays.asList("http://localhost:9000/oauth2/token"));
        nestedParams.put("authorization", Arrays.asList("OAuth oauth_consumer_key=\"cdadb3e7-7d30-4095-86e2-eac2afa7eb34\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"1470144202\",oauth_nonce=\"cpjNE9\",oauth_version=\"1.0\",oauth_signature=\"zm0dMD3lBb5%2BbaR395J7SVjThoI%3D\""));
        nestedParams.put("grant_type", Arrays.asList("thing"));
        nestedParams.put("key",Arrays.asList("value with spaces are here"));

        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURL, nestedParams, null);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA1, nestedParams, null);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithQueryParams() throws Exception {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURL, null, params);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA1, null, params);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithFormAndQueryParams() throws Exception {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURL, params, params);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA1, params, params);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithBaseURLWithPort() throws Exception {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURLWithPort, params, params);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURLWithPort, timestamp, nonce, SignatureMethod.HMACSHA1, params, params);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithBaseURLWithNonStandardPort() throws Exception {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURLWithNonStandardPort, params, params);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURLWithNonStandardPort, timestamp, nonce, SignatureMethod.HMACSHA1, params, params);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testVerifySha1Signature() throws Exception {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURLWithNonStandardPort, params, params);

        boolean verified = SignatureCalculator.verifySignature(consumerKey, method, baseURLWithNonStandardPort, timestamp, nonce,
                SignatureMethod.HMACSHA1, params, params, expectedSignature, consumerSecret);
        assertTrue(verified);
    }

    @Test
    public void testSignatureHmacSha256() {
        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA256, null, null);

        assertEquals(simpleSha256, actual);
    }

    @Test
    public void testSignatureHmacSha256WithFormParams() {
        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA256, params, null);

        assertEquals(withFormParamSha256, actual);
    }

    @Test
    public void testSignatureHmacSha256WithFormAndQueryParams() {
        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA256, params, params);

        assertEquals(withFormAndQueryParamSha256, actual);
    }

    @Test
    public void testSignatureHmacSha256WithBaseUrlWithPort() {
        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURLWithPort, timestamp, nonce, SignatureMethod.HMACSHA256, params, params);

        assertEquals(withFormAndQueryParamSha256, actual);
    }

    @Test
    public void testVerifySha256Signature() {
        boolean verified = SignatureCalculator.verifySignature(consumerKey, method, baseURLWithPort, timestamp, nonce,
                SignatureMethod.HMACSHA256, params, params, withFormAndQueryParamSha256, consumerSecret);
        assertTrue(verified);
    }

    @Test
    public void testVerifySha256UrlPort80SignatureToIncreaseCodeCoverage() {
        boolean verified = SignatureCalculator.verifySignature(consumerKey, method, baseURLWithPort80, timestamp, nonce,
                SignatureMethod.HMACSHA256, params, params, withFormAndQueryParamSha256, consumerSecret);
        assertFalse(verified);
    }

    /////////////////////////////////////// ES512 Tests //////////////////////////////////////////////////////////////////

    @Test
    public void testSignatureES512() {
        KeyPair pair = generateES512KeyPair();

        final byte[] keyBytes = pair.getPrivate().getEncoded();
        String keyBase64 = Base64.getEncoder().encodeToString(keyBytes);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, keyBase64);
        String signature = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.ES512, null, null);

        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        assertTrue(SignatureCalculator.verifySignature(consumerKey, method, baseURLWithPort, timestamp, nonce, SignatureMethod.ES512, null, null, signature, publicKeyBase64));
    }

    @Test
    public void testSignatureES512WithUserConsumerKey() {
        KeyPair pair = generateES512KeyPair();

        final byte[] keyBytes = pair.getPrivate().getEncoded();
        String keyBase64 = Base64.getEncoder().encodeToString(keyBytes);

        String consumerKey = "hrn:here-dev:account::test:HERE-a7a3b092-b8f3-4895-a918-8f7b368b0e73";

        SignatureCalculator sc = new SignatureCalculator(consumerKey, keyBase64);
        String signature = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.ES512, null, null);

        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        assertTrue(SignatureCalculator.verifySignature(consumerKey, method, baseURLWithPort, timestamp, nonce, SignatureMethod.ES512, null, null, signature, publicKeyBase64));

        String authHeader = sc.constructAuthHeader(signature, nonce, timestamp, SignatureMethod.ES512);
        // signature changes each time with ES512
        String encodedOauthConsumerKey = SignatureCalculator.urlEncode(consumerKey);
        String encodedSignature = SignatureCalculator.urlEncode(signature);
        String expectedAuthHeader = "OAuth oauth_consumer_key=\""
                + encodedOauthConsumerKey
                + "\", oauth_signature_method=\"ES512\", oauth_signature=\""
                + encodedSignature
                + "\", oauth_timestamp=\"123456789\", oauth_nonce=\"ab1Xo3\", oauth_version=\"1.0\"";
        assertTrue("expected authHeader " + expectedAuthHeader + ", actual " + authHeader,
                expectedAuthHeader.equals(authHeader));
    }


    @Test
    public void testSignatureES512WithBaseUrlWithPort(){
        KeyPair pair = generateES512KeyPair();

        final byte[] keyBytes = pair.getPrivate().getEncoded();
        String keyBase64 = Base64.getEncoder().encodeToString(keyBytes);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, keyBase64);
        String signature = sc.calculateSignature(method, baseURLWithPort, timestamp, nonce, SignatureMethod.ES512, params, params);

        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        boolean verified = SignatureCalculator.verifySignature(consumerKey, method, baseURLWithPort, timestamp, nonce, SignatureMethod.ES512, params, params, signature, publicKeyBase64);
        assertTrue(verified);
    }

    @Test
    public void testVerifyES512() {
        String cipherText = "testing public key and signature encryption";
        String publicKeyBase64 = "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQA2GcrZ94UbrYCsJo6sHOCnw4r5t5xSuX0x3LZPlRxA8dXziN1f1z2qUnudmI69JeEeX8JAfuu8kvRx4bjYnTVIasAO9P2V9eWWOxgfzGaC09JGBFN48XgI++9JNuS50DiHtSeSuM2kYTehHhj22Bj5iNlju1j1BbsAc1PS79G2pOpoFs=";
        String signature = "MIGIAkIAz0ZVhsjWnbmdZkBHP7wl5u5q4qN1K5bFgHNRvZeh4lYxpuUg60vncYZLwBM4zHev1F4bSkLqudhtAt8arwrLs1YCQgFQrQXvoSsAfO/gK7IEQXEFK1UGN4RDnVQRpKaZiKDbOCY2qZ3AyGeaydrnoc6o0RdHzeuJaj9Or2YjqE7PjnwvSg==";
        assertTrue(SignatureCalculator.verifySignature(cipherText, SignatureMethod.ES512, signature, publicKeyBase64));
    }

    public static KeyPair generateES512KeyPair()  {
        try {
            ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp521r1");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ELLIPTIC_CURVE_ALGORITHM);
            kpg.initialize(ecGenParameterSpec);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String computeSHA1SignatureUsingLibrary(String url, Map<String, List<String>> formParams, Map<String, List<String>> queryParams) throws Exception {
        Method computeSignature = OAuthSignatureCalculatorInstance.class.getDeclaredMethod("computeSignature", ConsumerKey.class, RequestToken.class, Uri.class, String.class, List.class, List.class, long.class, String.class);
        computeSignature.setAccessible(true);
        return (String) computeSignature.invoke(new OAuthSignatureCalculatorInstance(), new ConsumerKey(consumerKey, consumerSecret), new RequestToken(null, ""), Uri.create(url), method, toParamList(formParams), toParamList(queryParams), timestamp, Utf8UrlEncoder.percentEncodeQueryElement(nonce));
    }

    private static List<Param> toParamList(Map<String, List<String>> paramMap) {
        if (paramMap == null || paramMap.isEmpty()) return null;
        List<Param> paramList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : paramMap.entrySet()) {
            for (String value : entry.getValue()) {
                paramList.add(new Param(entry.getKey(), value));
            }
        }
        return paramList;
    }

    private static Map<String, List<String>> createParamsList() {
        List<String> values1 = new ArrayList<>();
        values1.add("value1");
        values1.add("value2");

        List<String> values2= new ArrayList<>();
        values2.add("value1");

        Map<String, List<String>> params = new HashMap<>();
        params.put("key1", values1);
        params.put("key2", values2);

        return params;
    }

}
