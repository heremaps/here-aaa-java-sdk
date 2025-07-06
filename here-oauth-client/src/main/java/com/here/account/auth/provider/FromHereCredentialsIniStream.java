package com.here.account.auth.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;

import com.here.account.util.Clock;
import com.here.account.util.SettableSystemClock;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.util.OAuthConstants;

public class FromHereCredentialsIniStream extends ClientCredentialsGrantRequestProvider
implements ClientAuthorizationRequestProvider {

    private final Clock clock;
    private final ClientAuthorizationRequestProvider delegate;

    public FromHereCredentialsIniStream(Clock clock, InputStream inputStream) {
        this(clock, inputStream, DEFAULT_INI_SECTION_NAME);
    }

    public FromHereCredentialsIniStream(InputStream inputStream) {
        this(inputStream, DEFAULT_INI_SECTION_NAME);
    }
    
    public FromHereCredentialsIniStream(InputStream inputStream, String sectionName) {
        this(new SettableSystemClock(), inputStream, sectionName);
    }

    public FromHereCredentialsIniStream(Clock clock, InputStream inputStream, String sectionName) {
        super(clock);
        Objects.requireNonNull(inputStream, "inputStream is required");

        this.clock = clock;
        // the delegate is fixed, because you cannot rewind an inputStream
        this.delegate = getClientCredentialsProvider(clock, inputStream, sectionName);
    }
    
    protected ClientAuthorizationRequestProvider getDelegate() {
        return delegate;
    }

    /**
     * @deprecated use {@link #getClientCredentialsProvider(Clock, InputStream, String)}
     * @param inputStream the input stream
     * @param sectionName the section name
     * @return the ClientAuthorizationRequestProvider
     */
    protected static ClientAuthorizationRequestProvider getClientCredentialsProvider(InputStream inputStream,
                                                                                     String sectionName) {
        return getClientCredentialsProvider(new SettableSystemClock(), inputStream, sectionName);
    }

    protected static ClientAuthorizationRequestProvider getClientCredentialsProvider(Clock clock, InputStream inputStream,
                                                                                     String sectionName) {
        try {
            Properties properties = getPropertiesFromIni(inputStream, sectionName);
            return FromSystemProperties.getClientCredentialsProviderWithDefaultTokenEndpointUrl(clock, properties);
        } catch (IOException | ConfigurationException e) {
            throw new RequestProviderException("trouble FromFile " + e, e);
        }
    }

    static final String DEFAULT_INI_SECTION_NAME = "default";

    static Properties getPropertiesFromIni(InputStream inputStream, String sectionName) throws IOException, ConfigurationException {
        try (Reader reader = new InputStreamReader(inputStream, OAuthConstants.UTF_8_CHARSET)) {
            INIConfiguration ini = new INIConfiguration();
            ini.read(reader);
            HierarchicalConfiguration<ImmutableNode> section = ini.getSection(sectionName);
            Properties properties = new Properties();
            Iterator<String> it = section.getKeys();
            while (it.hasNext()) {
                String key = it.next();
                String value = section.getString(key);
                switch (key.replaceAll("\\.+", ".")) {
                    case OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY:
                        properties.put(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY, value);
                        break;
                    case OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY:
                        properties.put(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY, value);
                        break;
                    case OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY:
                        properties.put(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY, value);
                        break;
                    case OAuth1ClientCredentialsProvider.FromProperties.TOKEN_SCOPE_PROPERTY:
                        properties.put(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_SCOPE_PROPERTY, value);
                        break;
                }
            }
            return properties;
        }
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
    public String getScope() {
        return getDelegate().getScope();
    }
}
