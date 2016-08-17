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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.here.account.util.OAuthConstants;

/**
 * Compute OAuth1.0 signature using the given parameters.
 * This class is specific to HERE Account.
 * 
 * @author srrajago
 */
public class SignatureCalculator {
    private String consumerKey;
    private String consumerSecret;

    public SignatureCalculator(String clientAccessKeyId, String clientAccessKeySecret) {
        this.consumerKey = clientAccessKeyId;
        this.consumerSecret = clientAccessKeySecret;
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
     * @param signatureMethod signature method to be used - supported are HMAC-SHA1 and HMAC-SHA256
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
     * @param signatureMethod signature method to be used - supported are HMAC-SHA1 and HMAC-SHA256
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

        //Create signature base with the http method and base url
        StringBuilder signatureBaseString = new StringBuilder(100);
        signatureBaseString.append(method.toUpperCase());
        signatureBaseString.append('&');
        signatureBaseString.append(urlEncode(normalizeBaseURL(baseURL)));

        //create parameter set with OAuth parameters
        OAuthParameterSet parameterSet = new OAuthParameterSet();
        parameterSet.add("oauth_consumer_key", this.consumerKey);
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
        return generateSignature(signatureBaseString.toString(), signatureMethod.getAlgorithm());
    }

    private String generateSignature(String signatureBaseString, String signatureMethod) {
        try {
            //get the bytes from the signature base string
            byte[] bytesToSign = signatureBaseString.getBytes(OAuthConstants.UTF_8_CHARSET);

            //create the signing key from the clientSecret
            byte[] keyBytes = (urlEncode(consumerSecret) + "&").getBytes(OAuthConstants.UTF_8_CHARSET);
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, signatureMethod);

            //generate signature based on the requested signature method
            Mac mac = Mac.getInstance(signatureMethod);
            mac.init(signingKey);
            byte[] signedBytes = mac.doFinal(bytesToSign);
            return Base64.encodeBase64String(signedBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Construct the OAuth 1.0 authorization header with the given parameters.
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
     * Remove the default port from the baseURL
     */
    private String normalizeBaseURL(String baseURL) {
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
     * Container class for Parameters.
     */
    static final private class OAuthParameterSet {

        private List<Parameter> allParameters = new ArrayList<>();

        /**
         * Add the given URL encoded key-value to the parameter list
         *
         * @param key   the parameter key
         * @param value the parameter value
         * @return the list with new parameter added.
         */
        public List<Parameter> add(String key, String value) {
            allParameters.add(new Parameter(urlEncode(key), urlEncode(value)));
            return allParameters;
        }

        /**
         * Sort the parameters by their key and concat into key=value format with '&'
         *
         * @return the concatinated parameters in the sorted order.
         */
        public String sortAndConcat() {
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
    static final private class Parameter implements Comparable<Parameter> {
        private String key;
        private String value;

        public Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        /**
         * Compare the key, if the key is the same, compare by the value.
         */
        public int compareTo(Parameter other) {
            int diff = this.key.compareTo(other.key);
            if (diff == 0) {
                diff = this.value.compareTo(other.value);
            }

            return diff;
        }
    }
}