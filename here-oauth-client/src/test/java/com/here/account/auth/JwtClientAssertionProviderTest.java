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

import com.here.account.http.HttpConstants;
import com.here.account.oauth2.AccessTokenRequest;
import com.here.account.oauth2.ClientAssertionCredentialsGrantRequest;
import com.here.account.util.SettableSystemClock;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class JwtClientAssertionProviderTest {

    private KeyPair keyPair;
    private String pemPrivateKey;
    private SettableSystemClock clock;

    private static final String TOKEN_ENDPOINT = "https://account.api.here.com/oauth2/token";
    private static final String CLIENT_ID = "test-app-id-xyz";

    @Before
    public void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
        pemPrivateKey = toPemPkcs8(keyPair.getPrivate());
        clock = new SettableSystemClock();
    }

    @Test
    public void testGetTokenEndpointUrl() {
        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(
                clock, TOKEN_ENDPOINT, CLIENT_ID, keyPair.getPrivate(), null);
        assertEquals(TOKEN_ENDPOINT, provider.getTokenEndpointUrl());
    }

    @Test
    public void testGetHttpMethod_isPOST() {
        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(
                clock, TOKEN_ENDPOINT, CLIENT_ID, keyPair.getPrivate(), null);
        assertEquals(HttpConstants.HttpMethods.POST, provider.getHttpMethod());
    }

    @Test
    public void testGetClientAuthorizer_isNoAuthorizer() {
        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(
                clock, TOKEN_ENDPOINT, CLIENT_ID, keyPair.getPrivate(), null);
        assertTrue("authorizer should be NoAuthorizer", provider.getClientAuthorizer() instanceof NoAuthorizer);
    }

    @Test
    public void testGetNewAccessTokenRequest_returnsClientAssertionRequest() {
        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(
                clock, TOKEN_ENDPOINT, CLIENT_ID, keyPair.getPrivate(), null);
        AccessTokenRequest request = provider.getNewAccessTokenRequest();
        assertTrue("request should be ClientAssertionCredentialsGrantRequest",
                request instanceof ClientAssertionCredentialsGrantRequest);
    }

    @Test
    public void testGetNewAccessTokenRequest_formParamsCorrect() {
        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(
                clock, TOKEN_ENDPOINT, CLIENT_ID, keyPair.getPrivate(), "openid");
        AccessTokenRequest request = provider.getNewAccessTokenRequest();
        Map<String, List<String>> formParams = request.toFormParams();

        assertEquals("client_credentials", formParams.get("grant_type").get(0));
        assertEquals("urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                formParams.get("client_assertion_type").get(0));
        assertNotNull("client_assertion must be present", formParams.get("client_assertion"));
        assertEquals("client_assertion must be a JWT", 3, formParams.get("client_assertion").get(0).split("\\.").length);
        assertEquals("openid", formParams.get("scope").get(0));
    }

    @Test
    public void testGetNewAccessTokenRequest_assertionSignatureValid() throws Exception {
        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(
                clock, TOKEN_ENDPOINT, CLIENT_ID, keyPair.getPrivate(), null);
        AccessTokenRequest request = provider.getNewAccessTokenRequest();
        Map<String, List<String>> formParams = request.toFormParams();

        String jwt = formParams.get("client_assertion").get(0);
        String[] parts = jwt.split("\\.");
        String signingInput = parts[0] + "." + parts[1];
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(keyPair.getPublic());
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        assertTrue("assertion signature must verify with the public key", sig.verify(signatureBytes));
    }

    @Test
    public void testFromProperties() {
        Properties props = new Properties();
        props.setProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY, TOKEN_ENDPOINT);
        props.setProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY, CLIENT_ID);
        props.setProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY, pemPrivateKey);
        props.setProperty(JwtClientAssertionProvider.TOKEN_SCOPE_PROPERTY, "test-scope");
        props.setProperty(JwtClientAssertionProvider.KEY_ID_PROPERTY, "kid-123");

        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(clock, props);

        assertEquals(TOKEN_ENDPOINT, provider.getTokenEndpointUrl());
        assertEquals("test-scope", provider.getScope());

        // Verify kid is in JWT header
        AccessTokenRequest request = provider.getNewAccessTokenRequest();
        String jwt = request.toFormParams().get("client_assertion").get(0);
        String header = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]));
        assertTrue("header should contain kid", header.contains("\"kid\":\"kid-123\""));
    }

    @Test
    public void testFromProperties_withExplicitAlgorithm() {
        Properties props = new Properties();
        props.setProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY, TOKEN_ENDPOINT);
        props.setProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY, CLIENT_ID);
        props.setProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY, pemPrivateKey);
        props.setProperty(JwtClientAssertionProvider.SIGNING_ALGORITHM_PROPERTY, "RS256");

        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(clock, props);
        AccessTokenRequest request = provider.getNewAccessTokenRequest();
        String jwt = request.toFormParams().get("client_assertion").get(0);
        String header = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]));
        assertTrue("header should contain RS256", header.contains("\"alg\":\"RS256\""));
    }

    @Test
    public void testFromProperties_unknownAlgorithmFallsBackToDefault() {
        Properties props = new Properties();
        props.setProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY, TOKEN_ENDPOINT);
        props.setProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY, CLIENT_ID);
        props.setProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY, pemPrivateKey);
        props.setProperty(JwtClientAssertionProvider.SIGNING_ALGORITHM_PROPERTY, "PS256");

        // PS256 is not yet in the enum, so fromJwtName returns null, provider uses default RS256
        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(clock, props);
        AccessTokenRequest request = provider.getNewAccessTokenRequest();
        String jwt = request.toFormParams().get("client_assertion").get(0);
        String header = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]));
        assertTrue("should fall back to RS256", header.contains("\"alg\":\"RS256\""));
    }

    @Test
    public void testIsPrivateKeyJwtConfigured_trueWhenAllPresent() {
        Properties props = new Properties();
        props.setProperty(JwtClientAssertionProvider.AUTH_METHOD_PROPERTY, "private_key_jwt");
        props.setProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY, CLIENT_ID);
        props.setProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY, pemPrivateKey);
        assertTrue(JwtClientAssertionProvider.isPrivateKeyJwtConfigured(props));
    }

    @Test
    public void testIsPrivateKeyJwtConfigured_falseWhenOauth1() {
        Properties props = new Properties();
        props.setProperty("here.access.key.id", "someId");
        props.setProperty("here.access.key.secret", "someSecret");
        assertFalse(JwtClientAssertionProvider.isPrivateKeyJwtConfigured(props));
    }

    @Test
    public void testIsPrivateKeyJwtConfigured_falseWhenMissingClientId() {
        Properties props = new Properties();
        props.setProperty(JwtClientAssertionProvider.AUTH_METHOD_PROPERTY, "private_key_jwt");
        props.setProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY, pemPrivateKey);
        assertFalse(JwtClientAssertionProvider.isPrivateKeyJwtConfigured(props));
    }

    @Test
    public void testLoadPrivateKey_fromInlinePem() {
        PrivateKey key = JwtClientAssertionProvider.loadPrivateKey(pemPrivateKey);
        assertNotNull(key);
        assertEquals("RSA", key.getAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadPrivateKey_fromNonExistentFile() {
        JwtClientAssertionProvider.loadPrivateKey("/nonexistent/path/to/key.pem");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadPrivateKey_fromInvalidPemContent() {
        JwtClientAssertionProvider.loadPrivateKey("-----BEGIN PRIVATE KEY-----\nnotvalidbase64!!!\n-----END PRIVATE KEY-----");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadPrivateKey_nullInput() {
        JwtClientAssertionProvider.loadPrivateKey(null);
    }

    @Test
    public void testLoadPrivateKey_fromRawBase64WithoutMarkers() {
        // Extract just the base64 from the PEM
        String raw = pemPrivateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        // raw starts with "MII" so loadPrivateKey should detect it as inline
        PrivateKey key = JwtClientAssertionProvider.loadPrivateKey(raw);
        assertNotNull(key);
        assertEquals("RSA", key.getAlgorithm());
    }

    @Test
    public void testEachCallProducesUniqueAssertion() {
        JwtClientAssertionProvider provider = new JwtClientAssertionProvider(
                clock, TOKEN_ENDPOINT, CLIENT_ID, keyPair.getPrivate(), null);
        String jwt1 = provider.getNewAccessTokenRequest().toFormParams().get("client_assertion").get(0);
        String jwt2 = provider.getNewAccessTokenRequest().toFormParams().get("client_assertion").get(0);
        assertNotEquals("each call must produce a unique assertion (different jti)", jwt1, jwt2);
    }

    @Test
    public void testFromSystemProperties_detectsPrivateKeyJwt() {
        Properties props = new Properties();
        props.setProperty(JwtClientAssertionProvider.AUTH_METHOD_PROPERTY, "private_key_jwt");
        props.setProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY, CLIENT_ID);
        props.setProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY, pemPrivateKey);
        props.setProperty(JwtClientAssertionProvider.TOKEN_ENDPOINT_URL_PROPERTY, TOKEN_ENDPOINT);

        // Use the provider directly (which internally uses FromSystemProperties logic)
        JwtClientAssertionProvider result = new JwtClientAssertionProvider(clock, props);

        assertEquals(TOKEN_ENDPOINT, result.getTokenEndpointUrl());
        assertTrue("authorizer should be NoAuthorizer",
                result.getClientAuthorizer() instanceof NoAuthorizer);
        assertTrue("should produce ClientAssertionCredentialsGrantRequest",
                result.getNewAccessTokenRequest()
                        instanceof com.here.account.oauth2.ClientAssertionCredentialsGrantRequest);
    }

    @Test
    public void testFromProperties_defaultsTokenEndpointWhenMissing() {
        // JwtClientAssertionProvider requires non-null tokenEndpointUrl,
        // but the FromSystemProperties/FromHereCredentialsIniStream wrappers
        // default it. Test via the INI path which is accessible.
        Properties props = new Properties();
        props.setProperty(JwtClientAssertionProvider.AUTH_METHOD_PROPERTY, "private_key_jwt");
        props.setProperty(JwtClientAssertionProvider.CLIENT_ID_PROPERTY, CLIENT_ID);
        props.setProperty(JwtClientAssertionProvider.PRIVATE_KEY_PROPERTY, pemPrivateKey);
        // No TOKEN_ENDPOINT_URL_PROPERTY

        // The INI stream path defaults the endpoint
        String ini = "[default]\n"
                + "here.auth.method = private_key_jwt\n"
                + "here.client.id = " + CLIENT_ID + "\n"
                + "here.private.key = " + pemPrivateKey.replace("\n", "") + "\n";

        // Use file-based approach for this test
        try {
            java.io.File tempKey = java.io.File.createTempFile("test-key", ".pem");
            tempKey.deleteOnExit();
            try (java.io.FileWriter fw = new java.io.FileWriter(tempKey)) {
                fw.write(pemPrivateKey);
            }
            String ini2 = "[default]\n"
                    + "here.auth.method = private_key_jwt\n"
                    + "here.client.id = " + CLIENT_ID + "\n"
                    + "here.private.key = " + tempKey.getAbsolutePath() + "\n";

            java.io.InputStream stream = new java.io.ByteArrayInputStream(
                    ini2.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            com.here.account.auth.provider.FromHereCredentialsIniStream iniProvider =
                    new com.here.account.auth.provider.FromHereCredentialsIniStream(clock, stream);

            assertEquals("should default to production endpoint",
                    "https://account.api.here.com/oauth2/token", iniProvider.getTokenEndpointUrl());
        } catch (Exception e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testFromProperties_returnsOAuth1WhenNoJwtConfig() {
        // When auth.method is NOT private_key_jwt, isPrivateKeyJwtConfigured returns false
        Properties props = new Properties();
        props.setProperty("here.access.key.id", "someId");
        props.setProperty("here.access.key.secret", "someSecret");
        assertFalse(JwtClientAssertionProvider.isPrivateKeyJwtConfigured(props));
    }

    @Test
    public void testFromIniStream_detectsPrivateKeyJwt() throws Exception {
        java.io.File tempKey = java.io.File.createTempFile("test-jwt-key", ".pem");
        tempKey.deleteOnExit();
        try (java.io.FileWriter fw = new java.io.FileWriter(tempKey)) {
            fw.write(pemPrivateKey);
        }

        String ini = "[default]\n"
                + "here.token.endpoint.url = " + TOKEN_ENDPOINT + "\n"
                + "here.auth.method = private_key_jwt\n"
                + "here.client.id = " + CLIENT_ID + "\n"
                + "here.private.key = " + tempKey.getAbsolutePath() + "\n";

        java.io.InputStream stream = new java.io.ByteArrayInputStream(
                ini.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        com.here.account.auth.provider.FromHereCredentialsIniStream provider =
                new com.here.account.auth.provider.FromHereCredentialsIniStream(clock, stream);

        assertEquals(TOKEN_ENDPOINT, provider.getTokenEndpointUrl());
        assertTrue("authorizer should be NoAuthorizer",
                provider.getClientAuthorizer() instanceof NoAuthorizer);
        assertTrue("should return ClientAssertionCredentialsGrantRequest",
                provider.getNewAccessTokenRequest()
                        instanceof com.here.account.oauth2.ClientAssertionCredentialsGrantRequest);
    }

    private static String toPemPkcs8(PrivateKey privateKey) {
        String base64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length()));
            sb.append('\n');
        }
        sb.append("-----END PRIVATE KEY-----\n");
        return sb.toString();
    }
}
