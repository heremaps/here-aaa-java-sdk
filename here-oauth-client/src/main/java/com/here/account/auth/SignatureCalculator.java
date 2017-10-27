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

import com.here.account.util.OAuthConstants;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.here.account.auth.SignatureMethod.ES512;

/**
 * Compute OAuth1.0 signature using the given parameters.
 * This class is specific to HERE Account.
 * 
 * @author srrajago
 */
public class SignatureCalculator {
    private final String consumerKey;
    private final String consumerSecret;

    /**
     * This is the constant for Elliptic Curve algorithm
     */
    public static final String ELLIPTIC_CURVE_ALGORITHM = "EC";

    public SignatureCalculator(String consumerKey, String consumerSecret) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    /**
     * Calculate the OAuth 1.0 signature based on the given parameters.
     * Same as 
     * {@link #calculateSignature(String, String, long, String, SignatureMethod, String, Map, Map)} 
     * but with oauthVersion hard-coded to "1.0".
     *
     * @param method          the HTTP method
     * @param baseURL         the base url including the protocol, host and port.
     * @param oauthTimestamp  the time stamp
     * @param nonce           nonce
     * @param signatureMethod signature method to be used - supported are HMAC-SHA1, HMAC-SHA256, ES512
     * @param formParams      the list of form parameters
     * @param queryParams     list of query parameters
     * @return computed signature using the requested signature method.
     */
    public String calculateSignature(String method, String baseURL, long oauthTimestamp,
                                     String nonce, SignatureMethod signatureMethod, 
                                     Map<String, List<String>> formParams, 
                                     Map<String, List<String>> queryParams) {
        return calculateSignature(method, baseURL, oauthTimestamp, nonce, signatureMethod,
                "1.0",
                formParams,
                queryParams);
    }
    
    /**
     * Calculate the OAuth 1.0 signature based on the given parameters
     *
     * @param method          the HTTP method
     * @param baseURL         the base url including the protocol, host and port.
     * @param oauthTimestamp  the time stamp
     * @param nonce           nonce
     * @param signatureMethod signature method to be used - supported are HMAC-SHA1, HMAC-SHA256, ES512
     * @param oauthVersion    the oauth_version value; 
     *                        OPTIONAL.  If present, MUST be set to "1.0".  Provides the
     *                        version of the authentication process as defined in RFC5849.
     * @param formParams      the list of form parameters
     * @param queryParams     list of query parameters
     * @return computed signature using the requested signature method.
     */
    public String calculateSignature(String method, String baseURL, long oauthTimestamp,
            String nonce, SignatureMethod signatureMethod, 
            String oauthVersion,
            Map<String, List<String>> formParams, 
            Map<String, List<String>> queryParams) {
        String signatureBaseString = computeSignatureBaseString(this.consumerKey, method, baseURL, oauthTimestamp, nonce, signatureMethod,
                oauthVersion,
                formParams,
                queryParams);
        return generateSignature(signatureBaseString.toString(), this.consumerSecret, signatureMethod);
    }

    /**
     * Construct the OAuth 1.0 authorization header with the given parameters. The oauth_version is set to "1.0"
     *
     * @param signature       the computed signature
     * @param nonce           nonce parameter
     * @param oauthTimestamp  timestamp parameter
     * @param signatureMethod signature method used to compute this header.
     * @return the Authorization header for OAuth 1.0 calls.
     */
    public String constructAuthHeader(String signature, String nonce, long oauthTimestamp, SignatureMethod signatureMethod) {
        return new StringBuilder().append("OAuth ")
                .append("oauth_consumer_key").append("=\"").append(consumerKey)
                .append("\", ").append("oauth_signature_method").append("=\"").append(signatureMethod.getOauth1SignatureMethod())
                .append("\", ").append("oauth_signature").append("=\"").append(urlEncode(signature))
                .append("\", ").append("oauth_timestamp").append("=\"").append(oauthTimestamp)
                .append("\", ").append("oauth_nonce").append("=\"").append(urlEncode(nonce))
                .append("\", ").append("oauth_version").append("=\"").append("1.0").append("\"").toString();
    }

    /**
     * Verify the signature. Compute the cipher text based on the given parameters and verify if the given signature is valid.
     * The oauth_version is set to "1.0" when computing the base string.
     *
     * @param consumerKey     the consumer key
     * @param method          the HTTP method
     * @param baseURL         the base url including the protocol, host and port.
     * @param oauthTimestamp  the time stamp
     * @param nonce           nonce
     * @param signatureMethod signature method to be used - supported are HMAC-SHA1, HMAC-SHA256, ES512
     * @param formParams      the list of form parameters
     * @param queryParams     list of query parameters
     * @param signatureToVerify the signature bytes to be verified.
     * @param verificationKey  the key used to verify the signature. This will be the shared secret key for HMAC-SHAn signature
     *                         method and is the public key for ES512 signature method.
     *
     * @return true if the signature was verified, false if not.
     */
    public static boolean verifySignature(String consumerKey, String method, String baseURL, long oauthTimestamp,
                                     String nonce, SignatureMethod signatureMethod,
                                     Map<String, List<String>> formParams,
                                     Map<String, List<String>> queryParams,
                                     String signatureToVerify,
                                     String verificationKey) {
        String signatureBaseString = computeSignatureBaseString(consumerKey, method, baseURL, oauthTimestamp, nonce, signatureMethod,
                "1.0",
                formParams,
                queryParams);
        return verifySignature(signatureBaseString, signatureMethod, signatureToVerify, verificationKey);

    }

    /**
     * Verify the signature.
     *
     * @param signedText    the original text that was signed.
     * @param signatureMethod signature method to be used - supported are HMAC-SHA1, HMAC-SHA256, ES512
     * @param signatureToVerify the signature bytes to be verified.
     * @param verificationKey  the key used to verify the signature. This will be the consumer key for HMAC-SHAn signature
     *                         method and is the public key for ES512 signature method.
     *
     * @return true if the signature was verified, false if not.
     */
    protected static boolean verifySignature(String signedText, SignatureMethod signatureMethod, String signatureToVerify, String verificationKey) {
        if (signatureMethod.equals(SignatureMethod.ES512))
            return verifyECDSASignature(signedText, signatureToVerify, verificationKey, signatureMethod);
        else
            return (generateSignature(signedText, verificationKey, signatureMethod).equals(signatureToVerify));
    }


    /**
     * Calculate the OAuth 1.0 signature base string based on the given parameters
     *
     * @param consumerKey     the consumer key
     * @param method          the HTTP method
     * @param baseURL         the base url including the protocol, host and port.
     * @param oauthTimestamp  the time stamp
     * @param nonce           nonce
     * @param signatureMethod signature method to be used - supported are HMAC-SHA1, HMAC-SHA256, ES512
     * @param oauthVersion    the oauth_version value;
     *                        OPTIONAL.  If present, MUST be set to "1.0".  Provides the
     *                        version of the authentication process as defined in RFC5849.
     * @param formParams      the list of form parameters
     * @param queryParams     list of query parameters
     * @return computed OAuth 1.0 signature base string.
     */
    private static String computeSignatureBaseString(String consumerKey, String method, String baseURL, long oauthTimestamp,
                                     String nonce, SignatureMethod signatureMethod,
                                     String oauthVersion,
                                     Map<String, List<String>> formParams,
                                     Map<String, List<String>> queryParams) {
        //Create signature base with the http method and base url
        StringBuilder signatureBaseString = new StringBuilder(100);
        signatureBaseString.append(method.toUpperCase());
        signatureBaseString.append('&');
        signatureBaseString.append(urlEncode(normalizeBaseURL(baseURL)));

        //create parameter set with OAuth parameters
        OAuthParameterSet parameterSet = new OAuthParameterSet();
        parameterSet.add("oauth_consumer_key", consumerKey);
        parameterSet.add("oauth_nonce", nonce);
        parameterSet.add("oauth_signature_method", signatureMethod.getOauth1SignatureMethod());
        parameterSet.add("oauth_timestamp", String.valueOf(oauthTimestamp));
        if (null != oauthVersion) {
            parameterSet.add("oauth_version", oauthVersion);
        }

        //add form parameters
        if (formParams != null && !formParams.isEmpty()) {
            for (String key : formParams.keySet()) {
                List<String> values = formParams.get(key);
                for (String value : values) {
                    parameterSet.add(key, value);
                }
            }
        }

        //add query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            for (String key : queryParams.keySet()) {
                List<String> values = queryParams.get(key);
                for (String value : values) {
                    parameterSet.add(key, value);
                }
            }
        }

        //sort the parameters by the key and format them into key=value concatenated with &
        String parameterString = parameterSet.sortAndConcat();
        //combine the signature base and parameters
        signatureBaseString.append('&');
        signatureBaseString.append(urlEncode(parameterString));
        return signatureBaseString.toString();
    }

    /**
     * Sign the cipher text using the given key and the specified algorithm
     * @param signatureBaseString the cipher text to be signed
     * @param key the signing key
     * @param signatureMethod signature method
     * @return signed cipher text
     */
    private static String generateSignature(String signatureBaseString, String key, SignatureMethod signatureMethod) {
        //get the bytes from the signature base string
        byte[] bytesToSign = signatureBaseString.getBytes(OAuthConstants.UTF_8_CHARSET);

        try {
            if (signatureMethod.equals(ES512))
                return computeECDSASignature(bytesToSign, key, signatureMethod.getAlgorithm());
            else
                return computeHMACSignature(bytesToSign, key, signatureMethod.getAlgorithm());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Utility method to URL encode a given string. If there are any spaces the URLEncodes encodes it to "+"
     * but we require it to be "%20".
     */
    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, OAuthConstants.UTF_8_STRING).replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Compute elliptic curve digital signature
     * @param bytesToSign bytes to be signed
     * @param algorithm elliptic curve algorithm to be used.
     * @return signed cipher text
     */
    private static String computeECDSASignature(byte[] bytesToSign, String key, String algorithm) {
        try {
            Signature s = Signature.getInstance(algorithm);
            s.initSign(consumerSecretToEllipticCurvePrivateKey(key));
            s.update(bytesToSign);
            return Base64.encodeBase64String(s.sign());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Compute HMAC digital signature
     * @param bytesToSign bytes to be signed
     * @param algorithm HMAC algorithm to be used.
     * @return signed cipher text
     */
    private static String computeHMACSignature(byte[] bytesToSign, String key, String algorithm) {
        try {
            byte[] keyBytes = (urlEncode(key) + "&").getBytes(OAuthConstants.UTF_8_CHARSET);
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, algorithm);

            //generate signature based on the requested signature method
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signingKey);
            byte[] signedBytes = mac.doFinal(bytesToSign);
            return Base64.encodeBase64String(signedBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Convert the consumer key to the elliptic curve private key
     */
    private static PrivateKey consumerSecretToEllipticCurvePrivateKey(String key) {
        try {
            byte[] keyBytes = Base64.decodeBase64(key);
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return kf.generatePrivate(privateSpec);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Remove the default port from the baseURL
     */
    private static String normalizeBaseURL(String baseURL) {
        int index;
        if (baseURL.startsWith("http:")) {
            index = baseURL.indexOf(":80/", 4);
            if (index > 0) {
                baseURL = baseURL.substring(0, index) + baseURL.substring(index + 3);
            }
        } else if (baseURL.startsWith("https:")) {
            index = baseURL.indexOf(":443/", 5);
            if (index > 0) {
                baseURL = baseURL.substring(0, index) + baseURL.substring(index + 4);
            }
        }

        return baseURL;
    }

    /**
     * Verify the Elliptic Curve signature.
     */
    private static boolean verifyECDSASignature(String cipherText, String signature, String verificationKey, SignatureMethod signatureMethod) {
        try {
            //convert the verification key to EC public key
            byte[] keyBytes = Base64.decodeBase64(verificationKey);
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance(ELLIPTIC_CURVE_ALGORITHM);
            PublicKey pubKey = kf.generatePublic(publicSpec);

            byte[] signatureBytes = Base64.decodeBase64(signature.getBytes(OAuthConstants.UTF_8_STRING));
            Signature s = Signature.getInstance(signatureMethod.getAlgorithm());
            s.initVerify(pubKey);
            s.update(cipherText.getBytes(OAuthConstants.UTF_8_STRING));
            return s.verify(signatureBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Container class for Parameters.
     */
    private static final class OAuthParameterSet {

        private final List<Parameter> allParameters = new ArrayList<>();

        /**
         * Add the given URL encoded key-value to the parameter list
         *
         * @param key   the parameter key
         * @param value the parameter value
         * @return the list with new parameter added.
         */
        private List<Parameter> add(String key, String value) {
            allParameters.add(new Parameter(urlEncode(key), urlEncode(value)));
            return allParameters;
        }

        /**
         * Sort the parameters by their key and concat into key=value format with '&'
         *
         * @return the concatinated parameters in the sorted order.
         */
        private String sortAndConcat() {
            Parameter[] params = new Parameter[allParameters.size()];
            allParameters.toArray(params);
            Arrays.sort(params);
            StringBuilder encodedParams = new StringBuilder(100);

            for (Parameter param : params) {
                if (encodedParams.length() > 0) {
                    encodedParams.append('&');
                }
                encodedParams.append(param.getKey()).append('=').append(param.getValue());
            }
            return encodedParams.toString();
        }
    }

    /**
     * Holds a tuple key-value pair.
     * Implements <code>Comparable</code> for sorting by the key.
     */
    private static final class Parameter implements Comparable<Parameter> {
        private final String key;
        private final String value;

        private Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

        private String getKey() {
            return key;
        }

        private String getValue() {
            return value;
        }

        /**
         * Compare the key, if the key is the same, compare by the value.
         */
        @Override
        public int compareTo(Parameter other) {
            int diff = this.key.compareTo(other.key);
            if (diff == 0) {
                diff = this.value.compareTo(other.value);
            }

            return diff;
        }
    }
}