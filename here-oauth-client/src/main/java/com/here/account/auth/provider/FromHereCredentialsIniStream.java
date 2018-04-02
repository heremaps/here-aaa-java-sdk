package com.here.account.auth.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import java.util.Properties;

import org.ini4j.Ini;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.util.OAuthConstants;

public class FromHereCredentialsIniStream extends ClientCredentialsGrantRequestProvider
implements ClientAuthorizationRequestProvider {

    private final ClientAuthorizationRequestProvider delegate;
    
    public FromHereCredentialsIniStream(InputStream inputStream) {
        this(inputStream, DEFAULT_INI_SECTION_NAME);
    }
    
    public FromHereCredentialsIniStream(InputStream inputStream, String sectionName) {
        Objects.requireNonNull(inputStream, "inputStream is required");
        
        // the delegate is fixed, because you cannot rewind an inputStream
        this.delegate = getClientCredentialsProvider(inputStream, sectionName);
    }
    
    protected ClientAuthorizationRequestProvider getDelegate() {
        return delegate;
    }
        
    protected static ClientAuthorizationRequestProvider getClientCredentialsProvider(InputStream inputStream, String sectionName) {
        try {
            Properties properties = getPropertiesFromIni(inputStream, sectionName);
            return FromSystemProperties.getClientCredentialsProviderWithDefaultTokenEndpointUrl(properties);
        } catch (IOException e) {
            throw new RequestProviderException("trouble FromFile " + e, e);
        }
    }

    static final String DEFAULT_INI_SECTION_NAME = "default";
    
    static Properties getPropertiesFromIni(InputStream inputStream, String sectionName) throws IOException {
        Ini ini = new Ini();
        try (Reader reader = new InputStreamReader(inputStream, OAuthConstants.UTF_8_CHARSET)) {
            ini.load(reader);
            Ini.Section section = ini.get(sectionName);
            Properties properties = new Properties();
            properties.put(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY,
                    section.get(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY));
            properties.put(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY,
                    section.get(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY));
            properties.put(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY,
                    section.get(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY));
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

}
