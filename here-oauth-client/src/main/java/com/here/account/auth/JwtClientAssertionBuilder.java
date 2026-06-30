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
import com.here.account.util.OAuthConstants;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds and signs JWT client assertions for use with OAuth 2.1 private_key_jwt
 * authentication per RFC 7523 Section 2.2.
 *
 * <p>
 * The produced JWT has:
 * <ul>
 *   <li>Header: {@code {"alg":"RS256","typ":"JWT"}}</li>
 *   <li>Claims: iss, sub (both = client_id), aud (token endpoint), exp, iat, jti</li>
 *   <li>Signature: RS256 (RSASSA-PKCS1-v1_5 with SHA-256)</li>
 * </ul>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7523#section-2.2">RFC 7523 §2.2</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7519">RFC 7519 (JWT)</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-13">OAuth 2.1</a>
 */
public class JwtClientAssertionBuilder {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * Default token lifetime: 5 minutes (300 seconds).
     * Per RFC 7523 §3, the assertion SHOULD have a short lifetime.
     */
    private static final long DEFAULT_EXPIRY_SECONDS = 300L;

    private final String clientId;
    private final String tokenEndpointUrl;
    private final PrivateKey privateKey;
    private final Clock clock;
    private long expirySeconds = DEFAULT_EXPIRY_SECONDS;
    private String kid;

    /**
     * Construct a new JWT client assertion builder.
     *
     * @param clock            the clock for timestamps
     * @param clientId         the client identifier (becomes iss and sub)
     * @param tokenEndpointUrl the token endpoint URL (becomes aud)
     * @param privateKey       the RSA private key for signing
     */
    public JwtClientAssertionBuilder(Clock clock, String clientId, String tokenEndpointUrl, PrivateKey privateKey) {
        Objects.requireNonNull(clock, "clock is required");
        Objects.requireNonNull(clientId, "clientId is required");
        Objects.requireNonNull(tokenEndpointUrl, "tokenEndpointUrl is required");
        Objects.requireNonNull(privateKey, "privateKey is required");

        this.clock = clock;
        this.clientId = clientId;
        this.tokenEndpointUrl = tokenEndpointUrl;
        this.privateKey = privateKey;
    }

    /**
     * Set a custom expiry duration for the assertion JWT.
     *
     * @param expirySeconds lifetime in seconds (must be positive, max recommended 300s)
     * @return this builder
     */
    public JwtClientAssertionBuilder setExpirySeconds(long expirySeconds) {
        if (expirySeconds <= 0) {
            throw new IllegalArgumentException("expirySeconds must be positive");
        }
        this.expirySeconds = expirySeconds;
        return this;
    }

    /**
     * Set the Key ID (kid) to include in the JWT header.
     * When the application has multiple registered JWKs, the kid helps the
     * authorization server identify which public key to use for verification.
     *
     * @param kid the key identifier (matches the kid registered on the app's JWK)
     * @return this builder
     */
    public JwtClientAssertionBuilder setKid(String kid) {
        this.kid = kid;
        return this;
    }

    /**
     * Build and sign a new JWT client assertion.
     *
     * <p>Per RFC 7523 §3, the JWT MUST contain:
     * <ul>
     *   <li>iss - the client_id</li>
     *   <li>sub - the client_id</li>
     *   <li>aud - the token endpoint URL</li>
     *   <li>exp - expiration time</li>
     *   <li>iat - issued at time</li>
     *   <li>jti - unique token identifier (prevents replay)</li>
     * </ul>
     *
     * @return the compact serialized JWT (header.payload.signature)
     * @throws ClientAssertionException if signing fails
     */
    public String buildAssertion() {
        long nowSeconds = clock.currentTimeMillis() / 1000L;
        long exp = nowSeconds + expirySeconds;
        String jti = UUID.randomUUID().toString();

        String header = buildHeaderJson();
        String payload = buildPayloadJson(nowSeconds, exp, jti);

        String headerEncoded = base64UrlEncode(header.getBytes(OAuthConstants.UTF_8_CHARSET));
        String payloadEncoded = base64UrlEncode(payload.getBytes(OAuthConstants.UTF_8_CHARSET));

        String signingInput = headerEncoded + "." + payloadEncoded;
        String signature = sign(signingInput);

        return signingInput + "." + signature;
    }

    private String buildHeaderJson() {
        if (kid != null && !kid.isEmpty()) {
            return "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"" + escapeJson(kid) + "\"}";
        }
        return "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
    }

    private String buildPayloadJson(long iat, long exp, String jti) {
        // Manual JSON construction to avoid dependency on serialization library for this simple case.
        // All string values are safe (no special chars that need escaping in client_id, URL, UUID).
        return "{" +
                "\"iss\":\"" + escapeJson(clientId) + "\"," +
                "\"sub\":\"" + escapeJson(clientId) + "\"," +
                "\"aud\":\"" + escapeJson(tokenEndpointUrl) + "\"," +
                "\"exp\":" + exp + "," +
                "\"iat\":" + iat + "," +
                "\"jti\":\"" + escapeJson(jti) + "\"" +
                "}";
    }

    private String sign(String signingInput) {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initSign(privateKey);
            sig.update(signingInput.getBytes(OAuthConstants.UTF_8_CHARSET));
            byte[] signatureBytes = sig.sign();
            return base64UrlEncode(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new ClientAssertionException("Failed to sign JWT client assertion: " + e.getMessage(), e);
        }
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Minimal JSON string escaping for known-safe values (client IDs, URLs, UUIDs).
     */
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Exception thrown when JWT client assertion building/signing fails.
     */
    public static class ClientAssertionException extends RuntimeException {
        public ClientAssertionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
