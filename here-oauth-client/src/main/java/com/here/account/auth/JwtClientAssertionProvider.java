/*
 * Copyright (c) 2025 HERE Europe B.V.
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

import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider;
import com.here.account.oauth2.AccessTokenRequest;
import com.here.account.oauth2.ClientAssertionCredentialsGrantRequest;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.util.Clock;
import com.here.account.util.OAuthConstants;
import com.here.account.util.SettableSystemClock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;

/**
 * A {@link ClientAuthorizationRequestProvider} that authenticates token requests
 * using JWT client assertions (private_key_jwt) per OAuth 2.1 and RFC 7523 §2.2.
 *
 * <p>
 * This provider:
 * <ul>
 *   <li>Uses {@link NoAuthorizer} — no Authorization header is sent (authentication is in the body)</li>
 *   <li>Generates a fresh signed JWT assertion for each token request via {@link JwtClientAssertionBuilder}</li>
 *   <li>Supports RS256 signing algorithm</li>
 * </ul>
 *
 * <p>
 * Credentials properties:
 * <ul>
 *   <li>{@value #TOKEN_ENDPOINT_URL_PROPERTY} - the token endpoint URL</li>
 *   <li>{@value #CLIENT_ID_PROPERTY} - the client identifier</li>
 *   <li>{@value #PRIVATE_KEY_PROPERTY} - PEM-encoded PKCS#8 RSA private key (inline or file path)</li>
 *   <li>{@value #TOKEN_SCOPE_PROPERTY} - (optional) token scope</li>
 *   <li>{@value #KEY_ID_PROPERTY} - (optional) key ID matching the registered JWK</li>
 *   <li>{@value #SIGNING_ALGORITHM_PROPERTY} - (optional) signing algorithm, defaults to RS256. Currently only RS256 is supported.</li>
 * </ul>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7523#section-2.2">RFC 7523 §2.2</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-13">OAuth 2.1</a>
 */
public class JwtClientAssertionProvider implements ClientAuthorizationRequestProvider {

    public static final String TOKEN_ENDPOINT_URL_PROPERTY = "here.token.endpoint.url";
    public static final String CLIENT_ID_PROPERTY = "here.client.id";
    public static final String PRIVATE_KEY_PROPERTY = "here.private.key";
    public static final String TOKEN_SCOPE_PROPERTY = "here.token.scope";
    public static final String AUTH_METHOD_PROPERTY = "here.auth.method";
    public static final String KEY_ID_PROPERTY = "here.key.id";
    public static final String SIGNING_ALGORITHM_PROPERTY = "here.signing.algorithm";

    /**
     * The auth method value that identifies private_key_jwt authentication.
     */
    public static final String AUTH_METHOD_PRIVATE_KEY_JWT = "private_key_jwt";

    private final Clock clock;
    private final String tokenEndpointUrl;
    private final String clientId;
    private final PrivateKey privateKey;
    private final String scope;
    private final String kid;
    private final JwtClientAssertionBuilder.SigningAlgorithm algorithm;
    private final NoAuthorizer noAuthorizer = new NoAuthorizer();

    /**
     * Construct a new JwtClientAssertionProvider.
     *
     * @param clock            the clock implementation
     * @param tokenEndpointUrl the token endpoint URL
     * @param clientId         the client identifier
     * @param privateKey       the RSA private key for signing assertions
     * @param scope            the optional token scope (may be null)
     */
    public JwtClientAssertionProvider(Clock clock, String tokenEndpointUrl, String clientId,
                                      PrivateKey privateKey, String scope) {
        this(clock, tokenEndpointUrl, clientId, privateKey, scope, null, null);
    }

    /**
     * Construct a new JwtClientAssertionProvider with key ID.
     *
     * @param clock            the clock implementation
     * @param tokenEndpointUrl the token endpoint URL
     * @param clientId         the client identifier
     * @param privateKey       the RSA private key for signing assertions
     * @param scope            the optional token scope (may be null)
     * @param kid              the optional key ID to include in JWT header (may be null)
     */
    public JwtClientAssertionProvider(Clock clock, String tokenEndpointUrl, String clientId,
                                      PrivateKey privateKey, String scope, String kid) {
        this(clock, tokenEndpointUrl, clientId, privateKey, scope, kid, null);
    }

    /**
     * Construct a new JwtClientAssertionProvider with key ID and algorithm.
     *
     * @param clock            the clock implementation
     * @param tokenEndpointUrl the token endpoint URL
     * @param clientId         the client identifier
     * @param privateKey       the RSA private key for signing assertions
     * @param scope            the optional token scope (may be null)
     * @param kid              the optional key ID to include in JWT header (may be null)
     * @param algorithm        the optional signing algorithm (may be null, defaults to RS256)
     */
    JwtClientAssertionProvider(Clock clock, String tokenEndpointUrl, String clientId,
                               PrivateKey privateKey, String scope, String kid,
                               JwtClientAssertionBuilder.SigningAlgorithm algorithm) {
        Objects.requireNonNull(clock, "clock is required");
        Objects.requireNonNull(tokenEndpointUrl, "tokenEndpointUrl is required");
        Objects.requireNonNull(clientId, "clientId is required");
        Objects.requireNonNull(privateKey, "privateKey is required");

        this.clock = clock;
        this.tokenEndpointUrl = tokenEndpointUrl;
        this.clientId = clientId;
        this.privateKey = privateKey;
        this.scope = scope;
        this.kid = kid;
        this.algorithm = algorithm;
    }

    /**
     * Construct from properties. Convenience constructor.
     *
     * @param clock      the clock implementation
     * @param properties properties containing token endpoint, client id, and private key
     */
    public JwtClientAssertionProvider(Clock clock, Properties properties) {
        this(clock,
                properties.getProperty(TOKEN_ENDPOINT_URL_PROPERTY),
                properties.getProperty(CLIENT_ID_PROPERTY),
                loadPrivateKey(properties.getProperty(PRIVATE_KEY_PROPERTY)),
                properties.getProperty(TOKEN_SCOPE_PROPERTY),
                properties.getProperty(KEY_ID_PROPERTY),
                JwtClientAssertionBuilder.SigningAlgorithm.fromJwtName(
                        properties.getProperty(SIGNING_ALGORITHM_PROPERTY)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenEndpointUrl() {
        return tokenEndpointUrl;
    }

    /**
     * Returns a {@link NoAuthorizer} since private_key_jwt authentication
     * is conveyed in the request body (client_assertion), not in the
     * Authorization header.
     *
     * {@inheritDoc}
     */
    @Override
    public HttpProvider.HttpRequestAuthorizer getClientAuthorizer() {
        return noAuthorizer;
    }

    /**
     * Builds a new {@link ClientAssertionCredentialsGrantRequest} with a freshly
     * signed JWT assertion.
     *
     * <p>
     * {@code scope} is baked into this provider at construction time because it is a
     * configuration-time concern (which project this provider is configured for).
     *
     * <p>
     * {@code resource} (RFC 8707) is intentionally NOT set here. It is a per-request
     * parameter identifying the target resource server for which the token is intended.
     * RFC 8707 anticipates callers varying {@code resource} per call, so it belongs on
     * the request, not the provider. Callers should set it on the returned request:
     * <pre>
     * {@code
     * AccessTokenRequest req = provider.getNewAccessTokenRequest();
     * req.setResource(Arrays.asList("https://api.example.com"));
     * tokenEndpoint.requestToken(req);
     * }
     * </pre>
     *
     * {@inheritDoc}
     */
    @Override
    public AccessTokenRequest getNewAccessTokenRequest() {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(
                clock, clientId, tokenEndpointUrl, privateKey);
        if (kid != null && !kid.isEmpty()) {
            builder.setKid(kid);
        }
        if (algorithm != null) {
            builder.setAlgorithm(algorithm);
        }
        String assertion = builder.buildAssertion();

        AccessTokenRequest request = new ClientAssertionCredentialsGrantRequest(assertion);
        request.setScope(scope);
        return request;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScope() {
        return scope;
    }

    /**
     * Load an RSA private key from PEM-encoded PKCS#8 content.
     * The value can be either:
     * <ul>
     *   <li>An inline PEM string (containing BEGIN/END markers or raw base64)</li>
     *   <li>A file path to a PEM file</li>
     * </ul>
     *
     * @param keyData the PEM string or file path
     * @return the RSA PrivateKey
     * @throws IllegalArgumentException if the key cannot be loaded or parsed
     */
    public static PrivateKey loadPrivateKey(String keyData) {
        Objects.requireNonNull(keyData, "private key data is required");

        String pemContent;
        if (keyData.contains("-----BEGIN") || keyData.contains("MII")) {
            // Inline PEM or raw base64
            pemContent = keyData;
        } else {
            // Treat as file path
            pemContent = readFileContent(keyData);
        }

        return parsePrivateKeyPem(pemContent);
    }

    /**
     * Parse a PEM-encoded PKCS#8 RSA private key.
     *
     * @param pem the PEM content
     * @return the PrivateKey
     */
    static PrivateKey parsePrivateKeyPem(String pem) {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to parse RSA private key: " + e.getMessage(), e);
        }
    }

    private static String readFileContent(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Private key file not found: " + filePath);
        }
        try (InputStream is = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, OAuthConstants.UTF_8_CHARSET))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read private key file: " + filePath, e);
        }
    }

    /**
     * Check if the given properties indicate private_key_jwt authentication.
     *
     * @param properties the properties to check
     * @return true if auth method is private_key_jwt and required fields are present
     */
    public static boolean isPrivateKeyJwtConfigured(Properties properties) {
        String authMethod = properties.getProperty(AUTH_METHOD_PROPERTY);
        return AUTH_METHOD_PRIVATE_KEY_JWT.equals(authMethod)
                && properties.getProperty(CLIENT_ID_PROPERTY) != null
                && properties.getProperty(PRIVATE_KEY_PROPERTY) != null;
    }

    /**
     * An implementation that loads credentials from a properties file.
     */
    public static class FromFile extends JwtClientAssertionProvider {
        public FromFile(File file) throws IOException {
            super(new SettableSystemClock(), getPropertiesFromFile(file));
        }

        public FromFile(Clock clock, File file) throws IOException {
            super(clock, getPropertiesFromFile(file));
        }

        private static Properties getPropertiesFromFile(File file) throws IOException {
            try (InputStream inputStream = new FileInputStream(file)) {
                Properties properties = new Properties();
                properties.load(inputStream);
                return properties;
            }
        }
    }
}
