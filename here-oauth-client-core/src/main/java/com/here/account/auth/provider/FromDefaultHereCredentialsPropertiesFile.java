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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.oauth2.ClientCredentialsProvider;
import com.here.account.util.Clock;
import com.here.account.util.SettableSystemClock;

/**
 * A {@link ClientCredentialsProvider} that pulls credential values from the
 * default "~/.here/credentials.properties" file.
 */
public class FromDefaultHereCredentialsPropertiesFile extends ClientCredentialsGrantRequestProvider 
implements ClientAuthorizationRequestProvider {

    private static final String CREDENTIALS_DOT_PROPERTIES_FILENAME = "credentials.properties";

    private final Clock clock;
    private final File file;

    public FromDefaultHereCredentialsPropertiesFile() {
        this(new SettableSystemClock());
    }

    public FromDefaultHereCredentialsPropertiesFile(Clock clock) {
        this(clock, getDefaultHereCredentialsFile());
    }
    
    public FromDefaultHereCredentialsPropertiesFile(File file) {
        this(new SettableSystemClock(), file);
    }

    public FromDefaultHereCredentialsPropertiesFile(Clock clock, File file) {
        this.clock = clock;
        this.file = file;
    }


    protected ClientCredentialsProvider getClientCredentialsProvider() {
        try {
            Properties properties = OAuth1ClientCredentialsProvider.getPropertiesFromFile(file);
            return FromSystemProperties.getClientCredentialsProviderWithDefaultTokenEndpointUrl(clock, properties
            );
        } catch (IOException e) {
            throw new RequestProviderException("trouble FromFile " + e, e);
        }
    }

    static File getDefaultHereCredentialsFile() {
        return DefaultHereConfigFiles.getDefaultHereConfigFile(CREDENTIALS_DOT_PROPERTIES_FILENAME);
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
    public HttpMethods getHttpMethod() {
        return HttpMethods.POST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clock getClock() {
        return clock;
    }

}
