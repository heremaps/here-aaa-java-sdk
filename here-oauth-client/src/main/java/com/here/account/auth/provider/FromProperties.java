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
package com.here.account.auth.provider;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpProvider;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.oauth2.ClientCredentialsProvider;
import com.here.account.util.Clock;

import java.util.Properties;

public class FromProperties extends ClientCredentialsGrantRequestProvider
        implements ClientAuthorizationRequestProvider {

    private final Properties properties;

    public FromProperties(Clock clock, String tokenEndpointUrl, String accessKeyId, String accessKeySecret) {
        this(clock, tokenEndpointUrl, accessKeyId, accessKeySecret, null);
    }

    public FromProperties(Clock clock, String tokenEndpointUrl, String accessKeyId, String accessKeySecret, String scope) {
        this(clock, getProperties(tokenEndpointUrl, accessKeyId, accessKeySecret, scope));
    }

    static Properties getProperties(String tokenEndpointUrl, String accessKeyId, String accessKeySecret) {
        return getProperties(tokenEndpointUrl, accessKeyId, accessKeySecret, null);
    }

    static Properties getProperties(String tokenEndpointUrl, String accessKeyId, String accessKeySecret, String scope) {
        Properties properties = new Properties();
        properties.put(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY, tokenEndpointUrl);
        properties.put(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY, accessKeyId);
        properties.put(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY, accessKeySecret);
        if (null != scope)
            properties.put(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_SCOPE_PROPERTY, scope);
        return properties;
    }

    public FromProperties(Clock clock, Properties properties) {
        super(clock);
        this.properties = properties;
    }

    protected ClientCredentialsProvider getDelegate() {
        return FromSystemProperties.getClientCredentialsProviderWithDefaultTokenEndpointUrl(getClock(), properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenEndpointUrl() {
        return getDelegate().getTokenEndpointUrl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpProvider.HttpRequestAuthorizer getClientAuthorizer() {
        return getDelegate().getClientAuthorizer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpConstants.HttpMethods getHttpMethod() {
        return HttpConstants.HttpMethods.POST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultScope() {
        return getDelegate().getDefaultScope();
    }
}
