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

import com.ning.http.client.FluentStringsMap;
import com.ning.http.client.oauth.ConsumerKey;
import com.ning.http.client.oauth.OAuthSignatureCalculator;
import com.ning.http.client.oauth.RequestToken;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class SignatureCalculatorTest {
    static final String consumerKey = "testKey";
    static final  String consumerSecret = "testSecret";
    static final  String nonce = "ab1Xo3";
    static final  Long timestamp = 123456789L;
    static final  String method = "POST";
    static final  String baseURL = "https://www.sampleurl.com/some/path";
    static final String baseURLWithPort = "https://www.sampleurl.com:443/some/path";
    static final String baseURLWithNonStandardPort = "https://www.sampleurl.com:9000/some/path";
    static final Map<String, List<String>> params = createParamsList();

    //expected signatures from SHA256 - obtained using the same parameters via postman.
    static final String simpleSha256 = "FeKRSQawLAt3GwVBYCJqY2lawFkEHs2u78MfT7m1P+A=";
    static final String withFormParamSha256 = "THFhaWB1Lbp/SVU+FSc/ix4CS6LpNY1suaZBSKQ4na4=";
    static final String withFormAndQueryParamSha256 = "bw5G7oIjsicO2Lp8rNa1H0bNj3Pkv+5aP2g9EedRpxI=";

    /////////////////////////////// HMAC-SHA1 //////////////////////////////////////////
    @Test
    public void testSignatureHmacSha1() {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURL, null, null);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA1, null, null);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithFormParams() {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURL, params, null);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA1, params, null);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithFormParamsWithSpacesInValue() {

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
    public void testSignatureHmacSha1WithQueryParams() {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURL, null, params);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA1, null, params);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithFormAndQueryParams() {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURL, params, params);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURL, timestamp, nonce, SignatureMethod.HMACSHA1, params, params);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithBaseURLWithPort() {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURLWithPort, params, params);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURLWithPort, timestamp, nonce, SignatureMethod.HMACSHA1, params, params);

        assertEquals(expectedSignature, actual);
    }

    @Test
    public void testSignatureHmacSha1WithBaseURLWithNonStandardPort() {
        String expectedSignature = computeSHA1SignatureUsingLibrary(baseURLWithNonStandardPort, params, params);

        SignatureCalculator sc = new SignatureCalculator(consumerKey, consumerSecret);
        String actual = sc.calculateSignature(method, baseURLWithNonStandardPort, timestamp, nonce, SignatureMethod.HMACSHA1, params, params);

        assertEquals(expectedSignature, actual);
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

    private static String computeSHA1SignatureUsingLibrary(String url, Map<String, List<String>> formParams, Map<String, List<String>> queryParams) {
        RequestToken emptyUserAuth = new RequestToken(null, "");
        OAuthSignatureCalculator calculator = new OAuthSignatureCalculator(new ConsumerKey(consumerKey, consumerSecret), emptyUserAuth);

        FluentStringsMap fluentFormParams = null;
        if (null != formParams && !formParams.isEmpty()) {
            fluentFormParams = new FluentStringsMap();
            fluentFormParams.putAll(formParams);
        }

        FluentStringsMap fluentQueryParams = null;
        if (null != queryParams && !queryParams.isEmpty()) {
            fluentQueryParams = new FluentStringsMap();
            fluentQueryParams.putAll(queryParams);
        }

        return calculator.calculateSignature(method, url, timestamp, nonce, fluentFormParams, fluentQueryParams);
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
