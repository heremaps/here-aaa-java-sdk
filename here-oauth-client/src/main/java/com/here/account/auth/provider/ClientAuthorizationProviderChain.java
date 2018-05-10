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
package com.here.account.auth.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.oauth2.AccessTokenRequest;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.oauth2.ClientCredentialsProvider;
import com.here.account.util.Clock;
import com.here.account.util.SettableSystemClock;

/**
 * @author kmccrack
 */
public class ClientAuthorizationProviderChain implements ClientAuthorizationRequestProvider {

    private static final Logger LOG = Logger.getLogger(ClientAuthorizationProviderChain.class.getName());
    private ClientAuthorizationRequestProvider mostRecentProvider = null;
    private List<ClientAuthorizationRequestProvider> clientAuthorizationProviders;

    public ClientAuthorizationProviderChain(ClientAuthorizationRequestProvider... clientAuthorizationProviders) {
        this.clientAuthorizationProviders = new ArrayList<ClientAuthorizationRequestProvider>();
        for (ClientAuthorizationRequestProvider clientAuthorizationProvider : clientAuthorizationProviders) {
            this.clientAuthorizationProviders.add(clientAuthorizationProvider);
        }
    }

    public ClientAuthorizationProviderChain(List<ClientCredentialsProvider> clientAuthorizationProviders) {
        this.clientAuthorizationProviders = new ArrayList<ClientAuthorizationRequestProvider>(clientAuthorizationProviders);
    }

    /**
     * @deprecated use {@link #getNewDefaultClientCredentialsProviderChain(Clock)}
     */
    @Deprecated
    public static ClientAuthorizationProviderChain DEFAULT_CLIENT_CREDENTIALS_PROVIDER_CHAIN =
            getNewDefaultClientCredentialsProviderChain(new SettableSystemClock());

    /**
     * Factory method for getting a new default ClientAuthorizationProviderChain.
     * The exact sequence of providers is subject to change in future releases, as
     * new providers are added.
     *
     * @param clock the clock implementation to use
     * @return the ClientAuthorizationProviderChain with default implementations in preference order
     */
    public static ClientAuthorizationProviderChain getNewDefaultClientCredentialsProviderChain(
            Clock clock
    ) {
        ClientAuthorizationRequestProvider systemProvider = new FromSystemProperties(clock);
        ClientAuthorizationRequestProvider iniFileProvider = new FromHereCredentialsIniFile(clock);
        ClientAuthorizationRequestProvider propertiesFileProvider = new FromDefaultHereCredentialsPropertiesFile(clock);
        return new ClientAuthorizationProviderChain(
                systemProvider,
                iniFileProvider,
                propertiesFileProvider
                );
    }

    protected ClientAuthorizationRequestProvider getClientCredentialsProvider() {
        if (mostRecentProvider != null) {
            return mostRecentProvider;
        }

        for (ClientAuthorizationRequestProvider credentials : clientAuthorizationProviders) {
            try {
                if (null != credentials.getTokenEndpointUrl() && credentials.getTokenEndpointUrl() != ""
                    && null != credentials.getClientAuthorizer()) {
                    LOG.info("Loading credentials from " + credentials.toString());

                    mostRecentProvider = credentials;
                    return credentials;
                }
            } catch (Exception e) {
                // Ignore any exceptions and move onto the next provider
                LOG.warning("Unable to load credentials from " + credentials.toString() +
                        ": " + e.getMessage());
            }
        }

        throw new RequestProviderException("Unable to load credentials from chain");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenEndpointUrl() {
        return getClientCredentialsProvider().getTokenEndpointUrl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpRequestAuthorizer getClientAuthorizer() {
        return getClientCredentialsProvider().getClientAuthorizer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenRequest getNewAccessTokenRequest() {
        return getClientCredentialsProvider().getNewAccessTokenRequest();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public HttpMethods getHttpMethod() {
        return getClientCredentialsProvider().getHttpMethod();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clock getClock() {
        return getClientCredentialsProvider().getClock();
    }


}
