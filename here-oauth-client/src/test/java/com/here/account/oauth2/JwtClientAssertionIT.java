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
package com.here.account.oauth2;

import com.here.account.auth.JwtClientAssertionBuilder;
import com.here.account.auth.JwtClientAssertionProvider;
import com.here.account.auth.NoAuthorizer;
import com.here.account.http.HttpProvider;
import com.here.account.http.java.JavaHttpProvider;
import com.here.account.util.Clock;
import com.here.account.util.SettableSystemClock;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.*;

/**
 * Integration test for OAuth 2.1 private_key_jwt client authentication.
 *
 * <p>Prerequisites (one-time manual setup):
 * <ol>
 *   <li>Generate an RSA key pair (see JwtTestSetupUtil)</li>
 *   <li>Register the public key (n, e) on the HERE Account server via
 *       POST /authentication/v1.1/apps/{appHRN}/jwks</li>
 *   <li>Create ~/.here/jwt-credentials.properties with:
 *       <pre>
 *       here.token.endpoint.url=https://stg.account.api.here.com/oauth2/token
 *       here.auth.method=private_key_jwt
 *       here.client.id=M406CQl58iM5RhpHgNNp
 *       here.private.key=/path/to/jwt-test-private-key.pem
 *       here.key.id=jwt-test-key-1
 *       </pre>
 *   </li>
 * </ol>
 *
 * <p>Run with: {@code mvn test -Dtest=JwtClientAssertionIT -DjwtCredentialsFile=/path/to/jwt-credentials.properties}
 */
public class JwtClientAssertionIT {

    private static final String JWT_CREDENTIALS_FILE_PROPERTY = "jwtCredentialsFile";
    private static final String DEFAULT_JWT_CREDENTIALS_PATH = System.getProperty("user.home")
            + File.separator + ".here" + File.separator + "jwt-credentials.properties";

    private HttpProvider httpProvider;
    private JwtClientAssertionProvider provider;
    private Properties props;
    private SettableSystemClock clock;

    @Before
    public void setUp() throws Exception {
        // Try system properties first (for CI side-loading)
        String sysPropClientId = System.getProperty("here.jwt.client.id");
        String sysPropPrivateKey = System.getProperty("here.jwt.private.key");
        String sysPropKeyId = System.getProperty("here.jwt.key.id");
        String sysPropTokenUrl = System.getProperty("here.token.endpoint.url",
                "https://stg.account.api.here.com/oauth2/token");
        String sysPropScope = System.getProperty("here.jwt.token.scope");

        if (sysPropClientId != null && sysPropPrivateKey != null) {
            props = new Properties();
            props.setProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY, sysPropTokenUrl);
            props.setProperty(JwtClientAssertionProvider.AUTH_METHOD_PROPERTY, "private_key_jwt");
            props.setProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY, sysPropClientId);
            props.setProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY, sysPropPrivateKey);
            if (sysPropKeyId != null) {
                props.setProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY, sysPropKeyId);
            }
            if (sysPropScope != null) {
                props.setProperty(JwtClientAssertionProvider.TOKEN_SCOPE_PROPERTY, sysPropScope);
            }
        } else {
            // Fall back to credentials file
            File credFile = getCredentialsFile();
            Assume.assumeTrue(
                    "Skipping JWT IT: no credentials found. Set -Dhere.jwt.client.id and -Dhere.jwt.private.key, "
                            + "or place credentials at " + credFile.getAbsolutePath(),
                    credFile.exists());

            props = new Properties();
            try (FileInputStream fis = new FileInputStream(credFile)) {
                props.load(fis);
            }
        }

        Assume.assumeTrue("Credentials must have here.auth.method=private_key_jwt",
                JwtClientAssertionProvider.isPrivateKeyJwtConfigured(props));

        clock = new SettableSystemClock();
        provider = new JwtClientAssertionProvider(clock, props);
        httpProvider = JavaHttpProvider.builder().build();
    }

    @After
    public void tearDown() throws IOException {
        if (httpProvider != null) {
            httpProvider.close();
        }
    }

    // ========================
    // Positive tests
    // ========================

    @Test
    public void test_clientAssertion_getAccessToken() {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, provider);
        AccessTokenResponse response = tokenEndpoint.requestToken(provider.getNewAccessTokenRequest());

        assertNotNull("response must not be null", response);
        String accessToken = response.getAccessToken();
        assertNotNull("accessToken must not be null", accessToken);
        assertFalse("accessToken must not be blank", accessToken.trim().isEmpty());
        assertEquals("tokenType must be bearer", "bearer", response.getTokenType());
        assertTrue("expiresIn must be positive", response.getExpiresIn() > 0);
    }

    @Test
    public void test_clientAssertion_withHereAccessTokenProvider() throws IOException {
        try (HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                .setClientAuthorizationRequestProvider(provider)
                .build()) {
            String accessToken = accessTokens.getAccessToken();
            assertNotNull("accessToken must not be null", accessToken);
            assertFalse("accessToken must not be blank", accessToken.trim().isEmpty());
        }
    }

    @Test
    public void test_clientAssertion_alwaysRequestNewToken() throws IOException {
        try (HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                .setClientAuthorizationRequestProvider(provider)
                .setAlwaysRequestNewToken(true)
                .build()) {
            String token1 = accessTokens.getAccessToken();
            String token2 = accessTokens.getAccessToken();
            assertNotNull(token1);
            assertNotNull(token2);
        }
    }

    @Test
    public void test_clientAssertion_customExpiresIn() {
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, provider);
        AccessTokenRequest request = provider.getNewAccessTokenRequest();
        request.setExpiresIn(15L);

        AccessTokenResponse response = tokenEndpoint.requestToken(request);
        assertNotNull(response);
        assertTrue("expiresIn should be <= 15", response.getExpiresIn() <= 15);
    }

    /**
     * Test: client_id is OPTIONAL in the request body.
     * The server identifies the client from the JWT's iss claim alone.
     */
    @Test
    public void test_clientAssertion_withoutClientIdInBody() {
        String clientId = props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String tokenEndpointUrl = props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);
        PrivateKey privateKey = JwtClientAssertionProvider.loadPrivateKey(
                props.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY));
        String kid = props.getProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY);

        // Build assertion manually — the form params will NOT include client_id
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, clientId, tokenEndpointUrl, privateKey);
        if (kid != null) {
            builder.setKid(kid);
        }
        String assertion = builder.buildAssertion();

        // Build form params without client_id — only grant_type, client_assertion_type, client_assertion
        Map<String, List<String>> formParams = new HashMap<>();
        formParams.put("grant_type", Collections.singletonList("client_credentials"));
        formParams.put("client_assertion_type",
                Collections.singletonList("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
        formParams.put("client_assertion", Collections.singletonList(assertion));

        // Send directly
        HttpProvider.HttpRequest httpRequest = httpProvider.getRequest(
                new NoAuthorizer(), "POST", tokenEndpointUrl, formParams);

        try {
            HttpProvider.HttpResponse response = httpProvider.execute(httpRequest);
            assertEquals("should succeed without client_id in body", 200, response.getStatusCode());
        } catch (Exception e) {
            fail("Request without client_id should succeed: " + e.getMessage());
        }
    }

    /**
     * Test: kid is OPTIONAL in the JWT header.
     * When absent, the server tries all registered JWKs for the app.
     */
    @Test
    public void test_clientAssertion_withoutKidInHeader() {
        String clientId = props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String tokenEndpointUrl = props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);
        PrivateKey privateKey = JwtClientAssertionProvider.loadPrivateKey(
                props.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY));

        // Build provider WITHOUT kid
        JwtClientAssertionProvider noKidProvider = new JwtClientAssertionProvider(
                clock, tokenEndpointUrl, clientId, privateKey, null, null);

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, noKidProvider);
        AccessTokenResponse response = tokenEndpoint.requestToken(noKidProvider.getNewAccessTokenRequest());

        assertNotNull("response must not be null", response);
        assertNotNull("accessToken must not be null", response.getAccessToken());
        assertFalse("accessToken must not be blank", response.getAccessToken().trim().isEmpty());
    }

    /**
     * Test: request a project-scoped token using the scope parameter.
     * Requires the app to be a member of the project specified in
     * the 'here.token.scope' property of the credentials file.
     *
     * <p>If here.token.scope is not configured, this test is skipped.
     */
    @Test
    public void test_clientAssertion_withScope() {
        String scope = props.getProperty(JwtClientAssertionProvider.TOKEN_SCOPE_PROPERTY);
        Assume.assumeTrue("Skipping scope test: here.token.scope not set in credentials",
                scope != null && !scope.isEmpty());

        String clientId = props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String tokenEndpointUrl = props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);
        PrivateKey privateKey = JwtClientAssertionProvider.loadPrivateKey(
                props.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY));
        String kid = props.getProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY);

        JwtClientAssertionProvider scopedProvider = new JwtClientAssertionProvider(
                clock, tokenEndpointUrl, clientId, privateKey, scope, kid);

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, scopedProvider);
        AccessTokenResponse response = tokenEndpoint.requestToken(scopedProvider.getNewAccessTokenRequest());

        assertNotNull("response must not be null", response);
        assertNotNull("accessToken must not be null", response.getAccessToken());
        assertFalse("accessToken must not be blank", response.getAccessToken().trim().isEmpty());
    }

    // ========================
    // Negative tests
    // ========================

    /**
     * Negative test: signing with a WRONG private key (not registered on server).
     * Expected: 401 - ClientAssertionSignatureInvalid (401952)
     */
    @Test
    public void test_clientAssertion_wrongKey_returns401() throws Exception {
        String clientId = props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String tokenEndpointUrl = props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);

        // Generate a random key pair NOT registered on the server
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair wrongKeyPair = kpg.generateKeyPair();

        JwtClientAssertionProvider wrongProvider = new JwtClientAssertionProvider(
                clock, tokenEndpointUrl, clientId, wrongKeyPair.getPrivate(), null, null);

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, wrongProvider);
        try {
            tokenEndpoint.requestToken(wrongProvider.getNewAccessTokenRequest());
            fail("Should have thrown AccessTokenException for wrong key");
        } catch (AccessTokenException e) {
            assertEquals("expected 401 status", 401, e.getStatusCode());
            // 401952 = ClientAssertionSignatureInvalid
            assertEquals("expected errorCode 401952", Integer.valueOf(401952), e.getErrorResponse().getErrorCode());
        }
    }

    /**
     * Negative test: expired JWT (iat and exp in the past).
     * Expected: 401 - ClientAssertionJwtExpired (401933)
     */
    @Test
    public void test_clientAssertion_expiredJwt_returns401() {
        String clientId = props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String tokenEndpointUrl = props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);
        PrivateKey privateKey = JwtClientAssertionProvider.loadPrivateKey(
                props.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY));
        String kid = props.getProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY);

        // Use a clock set 10 minutes in the past (assertion will be expired by the time server sees it)
        Clock pastClock = new Clock() {
            @Override
            public long currentTimeMillis() {
                return System.currentTimeMillis() - (10 * 60 * 1000L);
            }
            @Override
            public void schedule(ScheduledExecutorService s, Runnable r, long ms) {}
        };

        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(pastClock, clientId, tokenEndpointUrl, privateKey);
        if (kid != null) builder.setKid(kid);
        // Set expiry to 1 second so iat-10min + 1s = still well in the past
        builder.setExpirySeconds(1);
        String assertion = builder.buildAssertion();

        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(assertion);

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, provider);
        try {
            tokenEndpoint.requestToken(request);
            fail("Should have thrown AccessTokenException for expired JWT");
        } catch (AccessTokenException e) {
            assertEquals("expected 401 status", 401, e.getStatusCode());
            // 401933 = ClientAssertionJwtExpired
            assertEquals("expected errorCode 401933", Integer.valueOf(401933), e.getErrorResponse().getErrorCode());
        }
    }

    /**
     * Negative test: wrong audience (aud != token endpoint URL).
     * Expected: 400 or 401 - ClientAssertionAudInvalid (401932)
     */
    @Test
    public void test_clientAssertion_wrongAudience_returns401() {
        String clientId = props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String tokenEndpointUrl = props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);
        PrivateKey privateKey = JwtClientAssertionProvider.loadPrivateKey(
                props.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY));
        String kid = props.getProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY);

        // Build JWT with wrong audience
        String wrongAud = "https://wrong.example.com/oauth2/token";
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, clientId, wrongAud, privateKey);
        if (kid != null) builder.setKid(kid);
        String assertion = builder.buildAssertion();

        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(assertion);

        // We still need to POST to the real token endpoint
        JwtClientAssertionProvider realProvider = new JwtClientAssertionProvider(
                clock, tokenEndpointUrl, clientId, privateKey, null, kid);
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, realProvider);
        try {
            tokenEndpoint.requestToken(request);
            fail("Should have thrown AccessTokenException for wrong aud");
        } catch (AccessTokenException e) {
            // Server returns 400 or 401 for aud mismatch
            assertTrue("expected 400 or 401 status", e.getStatusCode() == 400 || e.getStatusCode() == 401);
        }
    }

    /**
     * Negative test: kid in JWT header does not match any registered JWK.
     * Expected: 401 - ClientAssertionKidNotFound (401953)
     */
    @Test
    public void test_clientAssertion_unknownKid_returns401() {
        String clientId = props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String tokenEndpointUrl = props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);
        PrivateKey privateKey = JwtClientAssertionProvider.loadPrivateKey(
                props.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY));

        // Use a kid that doesn't exist on the server
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, clientId, tokenEndpointUrl, privateKey);
        builder.setKid("non-existent-kid-xyz");
        String assertion = builder.buildAssertion();

        ClientAssertionCredentialsGrantRequest request = new ClientAssertionCredentialsGrantRequest(assertion);

        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, provider);
        try {
            tokenEndpoint.requestToken(request);
            fail("Should have thrown AccessTokenException for unknown kid");
        } catch (AccessTokenException e) {
            assertEquals("expected 401 status", 401, e.getStatusCode());
            // 401953 = ClientAssertionKidNotFound
            assertEquals("expected errorCode 401953", Integer.valueOf(401953), e.getErrorResponse().getErrorCode());
        }
    }

    /**
     * Negative test: replay the same JWT assertion twice.
     * The server uses jti for replay prevention (Redis SETNX).
     * Expected: 401 - ClientAssertionJTIRepeated (401934)
     */
    @Test
    public void test_clientAssertion_replayedJwt_returns401() {
        String clientId = props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String tokenEndpointUrl = props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);
        PrivateKey privateKey = JwtClientAssertionProvider.loadPrivateKey(
                props.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY));
        String kid = props.getProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY);

        // Build a single assertion
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, clientId, tokenEndpointUrl, privateKey);
        if (kid != null) builder.setKid(kid);
        String assertion = builder.buildAssertion();

        // First request should succeed
        ClientAssertionCredentialsGrantRequest request1 = new ClientAssertionCredentialsGrantRequest(assertion);
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, provider);
        AccessTokenResponse response = tokenEndpoint.requestToken(request1);
        assertNotNull("first request should succeed", response.getAccessToken());

        // Second request with the SAME assertion (same jti) should be rejected
        ClientAssertionCredentialsGrantRequest request2 = new ClientAssertionCredentialsGrantRequest(assertion);
        try {
            tokenEndpoint.requestToken(request2);
            fail("Should have thrown AccessTokenException for replayed JWT (same jti)");
        } catch (AccessTokenException e) {
            assertEquals("expected 401 status", 401, e.getStatusCode());
            // 401934 = ClientAssertionJTIRepeated
            assertEquals("expected errorCode 401934", Integer.valueOf(401934), e.getErrorResponse().getErrorCode());
        }
    }

    /**
     * Test: HereAccessTokenProvider.builder().build() picks up private_key_jwt
     * from system properties via the ClientAuthorizationProviderChain.
     * This simulates a production user setting -Dhere.auth.method=private_key_jwt etc.
     */
    @Test
    public void test_clientAssertion_viaProviderChainSystemProperties() throws IOException {
        // Set system properties that ClientAuthorizationProviderChain will detect
        String previousAuthMethod = System.getProperty(JwtClientAssertionProvider.AUTH_METHOD_PROPERTY);
        String previousClientId = System.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String previousPrivateKey = System.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY);
        String previousKeyId = System.getProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY);
        String previousTokenUrl = System.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);

        try {
            System.setProperty(JwtClientAssertionProvider.AUTH_METHOD_PROPERTY, "private_key_jwt");
            System.setProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY,
                    props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY));
            System.setProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY,
                    props.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY));
            System.setProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY,
                    props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY));
            String kid = props.getProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY);
            if (kid != null) {
                System.setProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY, kid);
            }

            // This is the production path: builder().build() should auto-detect private_key_jwt
            try (HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                    .setAlwaysRequestNewToken(true)
                    .build()) {
                String accessToken = accessTokens.getAccessToken();
                assertNotNull("accessToken must not be null", accessToken);
                assertFalse("accessToken must not be blank", accessToken.trim().isEmpty());
            }
        } finally {
            // Restore previous system properties
            restoreProperty(JwtClientAssertionProvider.AUTH_METHOD_PROPERTY, previousAuthMethod);
            restoreProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY, previousClientId);
            restoreProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY, previousPrivateKey);
            restoreProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY, previousKeyId);
            restoreProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY, previousTokenUrl);
        }
    }

    /**
     * Test: credentials loaded from INI format stream (simulates ~/.here/credentials.ini).
     */
    @Test
    public void test_clientAssertion_viaIniStream() throws IOException {
        String tokenUrl = props.getProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY);
        String clientId = props.getProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY);
        String privateKeyValue = props.getProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY);
        String keyId = props.getProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY);

        // Build INI content
        StringBuilder ini = new StringBuilder();
        ini.append("[default]\n");
        ini.append("here.token.endpoint.url = ").append(tokenUrl).append("\n");
        ini.append("here.auth.method = private_key_jwt\n");
        ini.append("here.client.id = ").append(clientId).append("\n");
        ini.append("here.private.key = ").append(privateKeyValue).append("\n");
        if (keyId != null) {
            ini.append("here.key.id = ").append(keyId).append("\n");
        }

        java.io.InputStream iniStream = new java.io.ByteArrayInputStream(
                ini.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

        com.here.account.auth.provider.FromHereCredentialsIniStream iniProvider =
                new com.here.account.auth.provider.FromHereCredentialsIniStream(clock, iniStream);

        try (HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                .setClientAuthorizationRequestProvider(iniProvider)
                .setAlwaysRequestNewToken(true)
                .build()) {
            String accessToken = accessTokens.getAccessToken();
            assertNotNull("accessToken must not be null", accessToken);
            assertFalse("accessToken must not be blank", accessToken.trim().isEmpty());
        }
    }

    private static void restoreProperty(String key, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previousValue);
        }
    }

    private File getCredentialsFile() {
        String path = System.getProperty(JWT_CREDENTIALS_FILE_PROPERTY);
        if (path != null && !path.isEmpty()) {
            return new File(path);
        }
        return new File(DEFAULT_JWT_CREDENTIALS_PATH);
    }
}
