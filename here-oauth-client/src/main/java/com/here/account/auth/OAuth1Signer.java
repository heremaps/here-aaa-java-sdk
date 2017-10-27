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

import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.util.Clock;
import org.apache.commons.codec.binary.Base64;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
    
    private final Clock clock;
    
    /**
     * HERE client accessKeyId.  Becomes the value of oauth_consumer_key in the 
     * Authorization: OAuth header.
     */
    private final String consumerKey;
    
    /**
     * HERE client accessKeySecret.  Used to calculate the oauth_signature in the 
     * Authorization: OAuth header.
     */
    private final String consumerSecret;
    
    
    private final SignatureMethod signatureMethod;
        
    /**
     * Construct the OAuth signer based on accessKeyId and accessKeySecret.
     * 
     * @param accessKeyId the HERE client accessKeyId.  Becomes the value of oauth_consumer_key in 
     *      the Authorization: OAuth header.
     * @param accessKeySecret the HERE client accessKeySecret.  Used to calculate the oauth_signature 
     *      in the Authorization: OAuth header.
     */
    public OAuth1Signer(String accessKeyId, String accessKeySecret) {
        this(Clock.SYSTEM, accessKeyId, accessKeySecret);
    }
    
    /**
     * Construct the OAuth signer based on clock, accessKeyId, and accessKeySecret.
     * Use this if you want to inject your own clock, such as during unit tests.
     * 
     * @param clock the implementation of a clock you want to use
     * @param accessKeyId the HERE clientId.  Becomes the value of oauth_consumer_key in 
     *      the Authorization: OAuth header.
     * @param accessKeySecret the HERE clientSecret.  Used to calculate the oauth_signature 
     *      in the Authorization: OAuth header.
     */
    public OAuth1Signer(Clock clock, String accessKeyId, String accessKeySecret) {
        this(clock, accessKeyId, accessKeySecret, SignatureMethod.HMACSHA256);
    }
    
    /**
     * 
     * @param consumerKey the identity of the caller, sent in plaintext.  
     *      Becomes the value of oauth_consumer_key in 
     *      the Authorization: OAuth header.
     * @param consumerSecret secret of the caller, or private key of the caller.  
     *      Used to calculate the oauth_signature 
     *      in the Authorization: OAuth header.
     * @param signatureMethod the choice of signature algorithm to use.
     */
    public OAuth1Signer(String consumerKey, String consumerSecret, SignatureMethod signatureMethod) {
        this(Clock.SYSTEM, consumerKey, consumerSecret, signatureMethod);
    }
    
    /**
     * Construct the OAuth signer based on clock, consumerKey, consumerSecret, 
     * and signatureMethod.
     * 
     * @param clock the implementation of a clock you want to use
     * @param consumerKey the identity of the caller, sent in plaintext.  
     *      Becomes the value of oauth_consumer_key in 
     *      the Authorization: OAuth header.
     * @param consumerSecret secret of the caller, or private key of the caller.  
     *      Used to calculate the oauth_signature 
     *      in the Authorization: OAuth header.
     * @param signatureMethod the choice of signature algorithm to use.
     */
    public OAuth1Signer(Clock clock, String consumerKey, String consumerSecret, SignatureMethod signatureMethod) {
        this.clock = clock;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.signatureMethod = signatureMethod;
    }

    /**
     * The source of entropy for OAuth1.0 nonce values.
     * File bytes with entropy for OAuth1.0 nonce values.
     * Note the OAuth1.0 spec specifically tells us we do not need to use a SecureRandom 
     * number generator.
     * 
     * @param bytes the byte array in which to stick the nonce value
     */
    protected void nextBytes(byte[] bytes) {
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
     * Note that the client accessKeySecret, once configured on this object, does not leave this method, 
     * as signatures are used in its place on the wire.
     * 
     * @param method
     * @return
     */
    private String getAuthorizationHeaderValue(String method, String url, 
            Map<String, List<String>> formParams) {
        SignatureCalculator calculator = getSignatureCalculator();
        
        // <a href="https://tools.ietf.org/html/rfc5849#section-3.3">timestamp</no I a>.
        // the number of seconds since January 1, 1970 00:00:00 GMT
        long timestamp = clock.currentTimeMillis() / 1000L;
        // choose the first 6 chars from base64 alphabet
        byte[] bytes = new byte[NONCE_LENGTH]; 
        nextBytes(bytes);
        String nonce = Base64.encodeBase64URLSafeString(bytes).substring(0, NONCE_LENGTH);
        String computedSignature = calculator.calculateSignature(method, url, timestamp, nonce, 
                signatureMethod,
                formParams,
                null);
        
        return calculator.constructAuthHeader(computedSignature, nonce, timestamp, 
                signatureMethod);
    }
    
    /**
     * Gets the signature calculator, given that we don't use a user auth, and we do use 
     * the configured client accessKeyId, client accessKeySecret pair.
     * 
     * @return
     */
    SignatureCalculator getSignatureCalculator() {
        // client accessKeyId is "Client Identifier" a.k.a. "oauth_consumer_key" in the OAuth1.0 spec
        // client accessKeySecret is "Client Shared-Secret" , which becomes the client shared-secret component 
        // of the HMAC-SHA1 key per http://tools.ietf.org/html/rfc5849#section-3.4.2.
        SignatureCalculator calculator = new SignatureCalculator(consumerKey, consumerSecret);
        return calculator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authorize(HttpRequest httpRequest, String method, String url, Map<String, List<String>> formParams) {
        String authorizationHeaderValue = getAuthorizationHeaderValue(method, url, formParams);
        httpRequest.addAuthorizationHeader(authorizationHeaderValue);
    }

}
