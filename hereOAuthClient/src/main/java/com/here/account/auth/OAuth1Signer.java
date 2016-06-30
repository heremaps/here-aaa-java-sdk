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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.codec.binary.Base64;

import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;
import com.ning.http.client.FluentStringsMap;
import com.ning.http.client.oauth.ConsumerKey;
import com.ning.http.client.oauth.OAuthSignatureCalculator;
import com.ning.http.client.oauth.RequestToken;

/**
 * Appends the 
 * <a href="https://tools.ietf.org/html/rfc5849">The OAuth 1.0 Protocol</a> 
 * signature to the HTTP request.
 * This request signer computes the signature components of the 
 * "OAuth" auth-scheme and adds them as the Authorization header value.
 * 
 * <p>
 * See also 
 * <a href="http://www.iana.org/assignments/http-authschemes/http-authschemes.xhtml#authschemes">
 * HTTP Authentication Scheme Registry</a> for a list of authschemes.
 * 
 * @author kmccrack
 */
public class OAuth1Signer implements HttpProvider.HttpRequestAuthorizer {
    
    /**
     * HERE Account recommends 6-character nonces.
     */
    private static final int NONCE_LENGTH = 6;
    
    /**
     * HERE clientId.  Becomes the value of oauth_consumer_key in the 
     * Authorization: OAuth header.
     */
    private final String clientId;
    
    /**
     * HERE clientSecret.  Used to calculate the oauth_signature in the 
     * Authorization: OAuth header.
     */
    private final String clientSecret;
    
    /**
     * Construct the OAuth signer based on clientId and clientSecret.
     * 
     * @param clientId the HERE clientId.  Becomes the value of oauth_consumer_key in 
     *      the Authorization: OAuth header.
     * @param clientSecret the HERE clientSecret.  Used to calculate the oauth_signature 
     *      in the Authorization: OAuth header.
     */
    public OAuth1Signer(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * The source of entropy for OAuth1.0 nonce values.
     * File bytes with entropy for OAuth1.0 nonce values.
     * Note the OAuth1.0 spec specifically tells us we do not need to use a SecureRandom 
     * number generator.
     * 
     * @return
     */
    private void nextBytes(byte[] bytes) {
        ThreadLocalRandom.current().nextBytes(bytes);;
    }

    /**
     * For cases where there is no Content-Type: application/x-www-form-urlencoded, 
     * and no request token, call this method to get the Authorization Header Value 
     * for a single request.  
     * 
     * <p>
     * Computes the OAuth1 Authorization header value including all required components of the 
     * OAuth type.
     * See also the OAuth 1.0
     * <a href="https://tools.ietf.org/html/rfc5849#section-3.5.1">Authorization Header</a>
     * Section.
     * 
     * <p>
     * Note that the clientSecret, once configured on this object, does not leave this method, 
     * as signatures are used in its place on the wire.
     * 
     * @param method
     * @return
     */
    private String getAuthorizationHeaderValue(String method, String url, 
            Map<String, List<String>> formParams) {
        OAuthSignatureCalculator calculator = getSignatureCalculator();
        
        // <a href="https://tools.ietf.org/html/rfc5849#section-3.3">timestamp</no I a>.
        // the number of seconds since January 1, 1970 00:00:00 GMT
        long timestamp = System.currentTimeMillis() / 1000L;
        // choose the first 6 chars from base64 alphabet
        byte[] bytes = new byte[NONCE_LENGTH]; 
        nextBytes(bytes);
        String nonce = Base64.encodeBase64URLSafeString(bytes).substring(0, NONCE_LENGTH);
        FluentStringsMap fluentFormParams = null;
        if (null != formParams && !formParams.isEmpty()) {
            fluentFormParams = new FluentStringsMap();
            fluentFormParams.putAll(formParams);
        }
        String computedSignature = calculator.calculateSignature(method, url, timestamp, nonce, 
                fluentFormParams,
                null);
        
        return calculator.constructAuthHeader(computedSignature, nonce, timestamp);
    }
    
    /**
     * Gets the signature calculator, given that we don't use a user auth, and we do use 
     * the configured clientId, clientSecret pair.
     * 
     * @return
     */
    OAuthSignatureCalculator getSignatureCalculator() {
        // https://tools.ietf.org/html/rfc5849#section-3.1" Making Requests
        // If the request is not associated with a resource owner
        // (no token available), clients MAY omit the parameter.
        RequestToken emptyUserAuth = new RequestToken(null, "");
        // clientId is "Client Identifier" a.k.a. "oauth_consumer_key" in the OAuth1.0 spec
        // clientSecret is "Client Shared-Secret" , which becomes the client shared-secret component 
        // of the HMAC-SHA1 key per http://tools.ietf.org/html/rfc5849#section-3.4.2.
        ConsumerKey consumerKey = new ConsumerKey(clientId, clientSecret);
        OAuthSignatureCalculator calculator = new OAuthSignatureCalculator(consumerKey, emptyUserAuth);
        return calculator;
    }

    @Override
    public void authorize(HttpRequest httpRequest, String method, String url, Map<String, List<String>> formParams) {
        String authorizationHeaderValue = getAuthorizationHeaderValue(method, url, formParams);
        httpRequest.addAuthorizationHeader(authorizationHeaderValue);
    }
    

}
