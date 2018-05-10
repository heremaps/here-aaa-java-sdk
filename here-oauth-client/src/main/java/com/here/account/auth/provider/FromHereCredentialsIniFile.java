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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.util.Clock;
import com.here.account.util.SettableSystemClock;

/**
 * @author kmccrack
 */
public class FromHereCredentialsIniFile extends ClientCredentialsGrantRequestProvider
implements ClientAuthorizationRequestProvider {

    private static final String CREDENTIALS_DOT_INI_FILENAME = "credentials.ini";

    private final Clock clock;
    private final File file;
    private final String sectionName;

    public FromHereCredentialsIniFile(Clock clock) {
        this(clock, getDefaultHereCredentialsIniFile(), FromHereCredentialsIniStream.DEFAULT_INI_SECTION_NAME);
    }

    public FromHereCredentialsIniFile() {
        this(getDefaultHereCredentialsIniFile(), FromHereCredentialsIniStream.DEFAULT_INI_SECTION_NAME);
    }

    public FromHereCredentialsIniFile(File file, String sectionName) {
        this(new SettableSystemClock(), file, sectionName);
    }

    public FromHereCredentialsIniFile(Clock clock, File file, String sectionName) {
        Objects.requireNonNull(clock, "clock is required");
        Objects.requireNonNull(file, "file is required");

        this.clock = clock;
        this.file = file;
        this.sectionName = sectionName;
    }
    
    /**
     * The delegate allows for reloading the file each time it is used, 
     * in case it has changed.
     * 
     * @return the ClientAuthorizationRequestProvider
     */
    protected ClientAuthorizationRequestProvider getDelegate() {
        try (InputStream inputStream = new FileInputStream(file)) {
            return new FromHereCredentialsIniStream(clock, inputStream, sectionName);
        } catch (IOException e) {
            throw new RequestProviderException("trouble FromFile " + e, e);
        }
    }
    
    protected File getFile() {
        return file;
    }

    protected static File getDefaultHereCredentialsIniFile() {
        return DefaultHereConfigFiles.getDefaultHereConfigFile(CREDENTIALS_DOT_INI_FILENAME);

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
    public HttpRequestAuthorizer getClientAuthorizer() {
        return getDelegate().getClientAuthorizer();
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
