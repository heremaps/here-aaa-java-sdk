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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import com.here.account.http.HttpProvider;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.oauth2.AccessTokenRequest;
import com.here.account.oauth2.ClientCredentialsGrantRequest;
import com.here.account.oauth2.ClientCredentialsProvider;
import com.here.account.util.Clock;
import com.here.account.util.SettableSystemClock;

/**
 * A {@link ClientCredentialsProvider} that injects client credentials by signing
 * token requests with an OAuth1 signature.
 */
public class OAuth1ClientCredentialsProvider implements ClientCredentialsProvider {

    private final Clock clock;
    private final String tokenEndpointUrl;
    private final OAuth1Signer oauth1Signer;
    
    /**
     * Construct a new {@code OAuth1ClientCredentialsProvider} that points to
     * the given token endpoint and uses the given client credentials to sign
     * requests using OAuth1 signatures.
     * 
     * @param tokenEndpointUrl the full URL of the OAuth2.0 token endpoint
     * @param accessKeyId the access key id to be used as a client credential
     * @param accessKeySecret the access key secret to be used as a client credential
     */
    public OAuth1ClientCredentialsProvider(String tokenEndpointUrl,
                                           String accessKeyId,
                                           String accessKeySecret) {
        this(new SettableSystemClock(), tokenEndpointUrl, accessKeyId, accessKeySecret);
    }

    public OAuth1ClientCredentialsProvider(Clock clock,
                                           String tokenEndpointUrl,
                                           String accessKeyId,
                                           String accessKeySecret
                                           ) {
        Objects.requireNonNull(clock, "clock is required");
        Objects.requireNonNull(tokenEndpointUrl, "tokenEndpointUrl is required");
        Objects.requireNonNull(accessKeyId, "accessKeyId is required");
        Objects.requireNonNull(accessKeySecret, "accessKeySecret is required");

        this.clock = clock;
        this.tokenEndpointUrl = tokenEndpointUrl;
        this.oauth1Signer = new OAuth1Signer(clock, accessKeyId, accessKeySecret);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clock getClock() {
        return clock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenEndpointUrl() {
        return tokenEndpointUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpProvider.HttpRequestAuthorizer getClientAuthorizer() {
        return oauth1Signer;
    }

    /**
     * An {@link OAuth1ClientCredentialsProvider} that pulls credentials values
     * from the given properties.  The properties supported and required are:
     * <ul>
     * <li>{@value #TOKEN_ENDPOINT_URL_PROPERTY} - Used to set the token endpoint URL.</li>
     * <li>{@value #ACCESS_KEY_ID_PROPERTY} - Used to set the access key id.</li>
     * <li>{@value #ACCESS_KEY_SECRET_PROPERTY} - Used to set the access key secret.</li>
     * </ul>
     */
    public static class FromProperties extends OAuth1ClientCredentialsProvider {
        
        public static final String TOKEN_ENDPOINT_URL_PROPERTY = "here.token.endpoint.url";
        public static final String ACCESS_KEY_ID_PROPERTY = "here.access.key.id";
        public static final String ACCESS_KEY_SECRET_PROPERTY = "here.access.key.secret";

        /**
         * Builds an {@link OAuth1ClientCredentialsProvider} by pulling the
         * required url, accessKeyId, and accessKeySecret from the given
         * properties.
         * 
         * @param properties the properties object to pull the required credentials from
         */
        public FromProperties(Properties properties) {
            super(properties.getProperty(TOKEN_ENDPOINT_URL_PROPERTY),
                  properties.getProperty(ACCESS_KEY_ID_PROPERTY),
                  properties.getProperty(ACCESS_KEY_SECRET_PROPERTY));
        }
    }

    /**
     * An {@link FromProperties} that pulls credential values from the specified File.
     */
    public static class FromFile extends FromProperties {
        
        /**
         * Builds an {@link OAuth1ClientCredentialsProvider} by pulling the
         * required url, accessKeyId, and accessKeySecret from the given
         * File.
         * 
         * @param file the File object to pull the required credentials from
         * @throws IOException if there is a problem loading the file
         */
        public FromFile(File file) throws IOException {
            super(getPropertiesFromFile(file));
        }
        
    }

    /**
     * Loads the File as an InputStream into a new Properties object,
     * and returns it.
     *
     * @param file the File to use as input
     * @return the Properties populated from the specified file's contents
     * @throws IOException if there is trouble reading the properties from the file
     */
    public static Properties getPropertiesFromFile(File file) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } finally {
            if (null != inputStream) {
                inputStream.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenRequest getNewAccessTokenRequest() {
        return new ClientCredentialsGrantRequest();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public HttpMethods getHttpMethod() {
        return HttpMethods.POST;
    }

}
