/*
 * Copyright (c) 2017 HERE Europe B.V.
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
package com.here.account.oauth2;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Supplier;

import com.here.account.auth.provider.ClientAuthorizationProviderChain;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;

/**
 * An implementation to get HERE Access Tokens, that is configured using its Builder.
 *
 * @author kmccrack
 */
public class HereAccessTokenProvider implements AccessTokenProvider, Closeable {

    /**
     * Gets a new Builder for a HERE Access Token Provider.
     *
     * @return the Builder
     * @see com.here.account.oauth2.HereAccessTokenProvider.Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * By default the Builder uses
     * <ul>
     *     <li>System properties</li>
     *     <li>~/.here/credentials.ini file</li>
     *     <li>~/.here/credentials.properties file</li>
     * </ul> for credentials,
     * the ApacheHttpClientProvider,
     * and the "always fresh" Access Token.
     */
    public static class Builder {
        private ClientAuthorizationRequestProvider clientAuthorizationRequestProvider;
        private HttpProvider httpProvider;
        private boolean alwaysRequestNewToken = false;
        
        private Builder() {
        }

        /**
         * Optionally set your custom ClientAuthorizationRequestProvider,
         * to override the default.
         *
         * @param clientAuthorizationRequestProvider the clientAuthorizationRequestProvider to set
         * @return this Builder
         */
        public Builder setClientAuthorizationRequestProvider(
                ClientAuthorizationRequestProvider clientAuthorizationRequestProvider) {
            this.clientAuthorizationRequestProvider = clientAuthorizationRequestProvider;
            return this;
        }

        /**
         * Optionally set your custom HttpProvider,
         * to override the default.
         *
         * @param httpProvider the HttpProvider to set
         * @return this Builder
         */
        public Builder setHttpProvider(HttpProvider httpProvider) {
            this.httpProvider = httpProvider;
            return this;
        }

        /**
         * Default is false.
         * It is not recommended to set this value to true, in a long-running
         * application.
         *
         * Optionally set this value to true, to make a remote API call
         * for every call to {@link HereAccessTokenProvider#getAccessToken()}.
         *
         * @param alwaysRequestNewToken default is false.  set to true to make
         *        every call to get an Access Token, be a
         *        remote API call.
         * @return this Builder
         */
        public Builder setAlwaysRequestNewToken(boolean alwaysRequestNewToken) {
            this.alwaysRequestNewToken = alwaysRequestNewToken;
            return this;
        }


        /**
         * Build using builders, builders, and more builders.
         *
         * @return the built HereAccessTokenProvider implementation for getting HERE Access Tokens.
         */
        public HereAccessTokenProvider build() {

            if (null == clientAuthorizationRequestProvider) {
                this.clientAuthorizationRequestProvider = 
                        ClientAuthorizationProviderChain.DEFAULT_CLIENT_CREDENTIALS_PROVIDER_CHAIN;
            }

            boolean doCloseHttpProvider = false;
            if (null == httpProvider) {
                // uses PoolingHttpClientConnectionManager by default
                this.httpProvider = ApacheHttpClientProvider.builder().build();
                // because the httpProvider was not injected, we should close it
                doCloseHttpProvider = true;
            }

            return new HereAccessTokenProvider(
                    clientAuthorizationRequestProvider,
                    httpProvider,
                    doCloseHttpProvider,
                    alwaysRequestNewToken);
        }
    }

    private final HttpProvider httpProvider;
    private final boolean doCloseHttpProvider;
    private final TokenEndpoint tokenEndpoint;
    private final Supplier<AccessTokenRequest> accessTokenRequestSupplier;
    private final Fresh<AccessTokenResponse> fresh;


    private HereAccessTokenProvider(ClientAuthorizationRequestProvider credentials, HttpProvider httpProvider,
            boolean doCloseHttpProvider, boolean alwaysRequestNewToken) {
        this.httpProvider = httpProvider;
        this.doCloseHttpProvider = doCloseHttpProvider;
        this.tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, credentials);
        this.accessTokenRequestSupplier = () -> {
            return credentials.getNewAccessTokenRequest();
        };
        if (alwaysRequestNewToken) {
            // always request a new token
            this.fresh = null;
        } else {
            // use the auto-refreshing technique
            this.fresh = tokenEndpoint.requestAutoRefreshingToken(
                    accessTokenRequestSupplier);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccessToken() {
        return getAccessTokenResponse().getAccessToken();
    }
    
    public AccessTokenResponse getAccessTokenResponse() {
        if (null != fresh) {
            return fresh.get();
        } else {
            return tokenEndpoint.requestToken(accessTokenRequestSupplier.get());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        try {
            if (null != fresh) {
                fresh.close();
            }
        } finally {
            if (doCloseHttpProvider && null != httpProvider) {
                httpProvider.close();
            }
        }
    }
}
