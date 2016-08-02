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

import com.here.account.http.HttpProvider;
import com.here.account.oauth2.ClientCredentialsProvider;
import java.util.Objects;
import java.util.Properties;

/**
 * A {@link ClientCredentialsProvider} that injects client credentials by signing
 * token requests with an OAuth1 signature.
 */
public class OAuth1ClientCredentialsProvider implements ClientCredentialsProvider {
    
    private final String tokenEndpointUrl;
    private final OAuth1Signer oauth1Signer;
    
    /**
     * Construct a new {@code OAuth1ClientCredentialsProvider} that points to
     * the given token endpoint and uses the given client credentials to sign
     * requests using OAuth1 signatures.
     * 
     * @param url the full URL of the OAuth2.0 token endpoint
     * @param accessKeyId the access key id to be used as a client credential
     * @param accessKeySecret the access key secret to be used as a client credential
     */
    public OAuth1ClientCredentialsProvider(String url,
                                           String accessKeyId,
                                           String accessKeySecret) {
        Objects.requireNonNull(url, "url is required");
        Objects.requireNonNull(accessKeyId, "accessKeyId is required");
        Objects.requireNonNull(accessKeySecret, "accessKeySecret is required");
        
        this.tokenEndpointUrl = url;
        this.oauth1Signer = new OAuth1Signer(accessKeyId, accessKeySecret);
    }

    @Override
    public String getTokenEndpointUrl() {
        return tokenEndpointUrl;
    }

    @Override
    public HttpProvider.HttpRequestAuthorizer getClientAuthorizer() {
        return oauth1Signer;
    }

    /**
     * An {@link OAuth1ClientCredentialsProvider} that pulls credentials values
     * from the given properties.  The properties supported and required are:
     * <ul>
     * <li>{@value #URL_PROPERTY} - Used to set the token endpoint URL.</li>
     * <li>{@value #ACCESS_KEY_ID_PROPERTY} - Used to set the access key id.</li>
     * <li>{@value #ACCESS_KEY_SECRET_PROPERTY} - Used to set the access key secret.</li>
     * </ul>
     */
    public static class FromProperties extends OAuth1ClientCredentialsProvider {
        
        public static final String URL_PROPERTY = "com.here.account.auth.token.endpoint.url";
        public static final String ACCESS_KEY_ID_PROPERTY = "com.here.account.auth.access.key.id";
        public static final String ACCESS_KEY_SECRET_PROPERTY = "com.here.account.auth.access.key.secret";

        /**
         * Builds an {@link OAuth1ClientCredentialsProvider} by pulling the
         * required url, accessKeyId, and accessKeySecret from the given
         * properties.
         * 
         * @param properties the properties object to pull the required credentials from
         */
        public FromProperties(Properties properties) {
            super(properties.getProperty(URL_PROPERTY),
                  properties.getProperty(ACCESS_KEY_ID_PROPERTY),
                  properties.getProperty(ACCESS_KEY_SECRET_PROPERTY));
        }
    }
}
