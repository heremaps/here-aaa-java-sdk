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

import com.here.account.util.Clock;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.*;

public class JwtClientAssertionBuilderTest {

    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private TestClock clock;

    private static final String CLIENT_ID = "test-client-id-12345";
    private static final String TOKEN_ENDPOINT = "https://account.api.here.com/oauth2/token";

    @Before
    public void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        clock = new TestClock(System.currentTimeMillis());
    }

    @Test
    public void testBuildAssertion_producesThreePartJwt() {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        String jwt = builder.buildAssertion();

        String[] parts = jwt.split("\\.");
        assertEquals("JWT must have 3 parts", 3, parts.length);
        assertFalse("header must not be empty", parts[0].isEmpty());
        assertFalse("payload must not be empty", parts[1].isEmpty());
        assertFalse("signature must not be empty", parts[2].isEmpty());
    }

    @Test
    public void testBuildAssertion_headerContainsRS256() {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        String jwt = builder.buildAssertion();

        String header = decodeBase64Url(jwt.split("\\.")[0]);
        assertTrue("header must contain RS256", header.contains("\"alg\":\"RS256\""));
        assertTrue("header must contain JWT type", header.contains("\"typ\":\"JWT\""));
    }

    @Test
    public void testBuildAssertion_headerContainsKidWhenSet() {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        builder.setKid("my-key-id-1");
        String jwt = builder.buildAssertion();

        String header = decodeBase64Url(jwt.split("\\.")[0]);
        assertTrue("header must contain kid", header.contains("\"kid\":\"my-key-id-1\""));
    }

    @Test
    public void testBuildAssertion_headerOmitsKidWhenNotSet() {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        String jwt = builder.buildAssertion();

        String header = decodeBase64Url(jwt.split("\\.")[0]);
        assertFalse("header must not contain kid when not set", header.contains("kid"));
    }

    @Test
    public void testBuildAssertion_payloadContainsRequiredClaims() {
        long nowMillis = 1700000000000L;
        clock.setCurrentTimeMillis(nowMillis);

        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        String jwt = builder.buildAssertion();

        String payload = decodeBase64Url(jwt.split("\\.")[1]);

        assertTrue("payload must contain iss", payload.contains("\"iss\":\"" + CLIENT_ID + "\""));
        assertTrue("payload must contain sub", payload.contains("\"sub\":\"" + CLIENT_ID + "\""));
        assertTrue("payload must contain aud", payload.contains("\"aud\":\"" + TOKEN_ENDPOINT + "\""));
        assertTrue("payload must contain iat", payload.contains("\"iat\":" + (nowMillis / 1000)));
        assertTrue("payload must contain exp", payload.contains("\"exp\":" + (nowMillis / 1000 + 300)));
        assertTrue("payload must contain jti", payload.contains("\"jti\":\""));
    }

    @Test
    public void testBuildAssertion_customExpirySeconds() {
        long nowMillis = 1700000000000L;
        clock.setCurrentTimeMillis(nowMillis);

        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        builder.setExpirySeconds(60);
        String jwt = builder.buildAssertion();

        String payload = decodeBase64Url(jwt.split("\\.")[1]);
        assertTrue("exp should be iat + 60", payload.contains("\"exp\":" + (nowMillis / 1000 + 60)));
    }

    @Test
    public void testBuildAssertion_signatureIsVerifiable() throws Exception {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        String jwt = builder.buildAssertion();

        String[] parts = jwt.split("\\.");
        String signingInput = parts[0] + "." + parts[1];
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        assertTrue("signature must verify with the corresponding public key", sig.verify(signatureBytes));
    }

    @Test
    public void testBuildAssertion_uniqueJtiPerCall() {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);

        String jwt1 = builder.buildAssertion();
        String jwt2 = builder.buildAssertion();

        String payload1 = decodeBase64Url(jwt1.split("\\.")[1]);
        String payload2 = decodeBase64Url(jwt2.split("\\.")[1]);

        // Extract jti values
        String jti1 = extractJsonValue(payload1, "jti");
        String jti2 = extractJsonValue(payload2, "jti");

        assertNotEquals("each assertion must have unique jti", jti1, jti2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetExpirySeconds_rejectsZero() {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        builder.setExpirySeconds(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetExpirySeconds_rejectsNegative() {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        builder.setExpirySeconds(-1);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_rejectsNullClientId() {
        new JwtClientAssertionBuilder(clock, null, TOKEN_ENDPOINT, privateKey);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_rejectsNullPrivateKey() {
        new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, null);
    }

    @Test
    public void testDefaultAlgorithm_isRS256() {
        JwtClientAssertionBuilder builder = new JwtClientAssertionBuilder(clock, CLIENT_ID, TOKEN_ENDPOINT, privateKey);
        String jwt = builder.buildAssertion();
        String header = decodeBase64Url(jwt.split("\\.")[0]);
        assertTrue("default algorithm must be RS256", header.contains("\"alg\":\"RS256\""));
    }

    @Test
    public void testFromJwtName_returnsRS256() {
        JwtClientAssertionBuilder.SigningAlgorithm alg = JwtClientAssertionBuilder.SigningAlgorithm.fromJwtName("RS256");
        assertNotNull(alg);
        assertEquals("RS256", alg.getJwtName());
        assertEquals("SHA256withRSA", alg.getJavaName());
    }

    @Test
    public void testFromJwtName_caseInsensitive() {
        JwtClientAssertionBuilder.SigningAlgorithm alg = JwtClientAssertionBuilder.SigningAlgorithm.fromJwtName("rs256");
        assertNotNull(alg);
        assertEquals("RS256", alg.getJwtName());
    }

    @Test
    public void testFromJwtName_nullReturnsNull() {
        assertNull(JwtClientAssertionBuilder.SigningAlgorithm.fromJwtName(null));
    }

    @Test
    public void testFromJwtName_unknownReturnsNull() {
        assertNull(JwtClientAssertionBuilder.SigningAlgorithm.fromJwtName("PS256"));
        assertNull(JwtClientAssertionBuilder.SigningAlgorithm.fromJwtName("UNKNOWN"));
    }

    private static String decodeBase64Url(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private static class TestClock implements Clock {
        private long millis;

        TestClock(long millis) {
            this.millis = millis;
        }

        void setCurrentTimeMillis(long millis) {
            this.millis = millis;
        }

        @Override
        public long currentTimeMillis() {
            return millis;
        }

        @Override
        public void schedule(ScheduledExecutorService scheduledExecutorService, Runnable runnable,
                             long millisecondsInTheFutureToSchedule) {
        }
    }
}
