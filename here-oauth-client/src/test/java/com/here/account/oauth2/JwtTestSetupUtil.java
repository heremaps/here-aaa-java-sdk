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

import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * One-time setup utility for OAuth 2.1 private_key_jwt integration testing.
 *
 * <p>This utility:
 * <ol>
 *   <li>Generates a 2048-bit RSA key pair</li>
 *   <li>Writes the private key PEM to a file</li>
 *   <li>Writes a jwt-credentials.properties template</li>
 *   <li>Prints the curl command to register the public JWK on HERE Account</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>
 *   mvn -DskipTests compile exec:java \
 *     -Dexec.mainClass="com.here.account.oauth2.JwtTestSetupUtil" \
 *     -Dexec.classpathScope="test" \
 *     -Dexec.args="YOUR_APP_HRN YOUR_BEARER_TOKEN [output_dir]"
 * </pre>
 *
 * Or simply run with:
 * <pre>
 *   java -cp target/test-classes:target/classes com.here.account.oauth2.JwtTestSetupUtil \
 *     hrn:here:account::HERE:app/YOUR_APP_ID \
 *     YOUR_BEARER_TOKEN \
 *     ~/.here
 * </pre>
 */
public class JwtTestSetupUtil {

    private static final String DEFAULT_OUTPUT_DIR = System.getProperty("user.home") + File.separator + ".here";
    private static final String DEFAULT_TOKEN_ENDPOINT = "https://account.api.here.com/oauth2/token";
    private static final String DEFAULT_HA_BASE_URL = "https://account.api.here.com";
    private static final String KID = "jwt-test-key-1";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: JwtTestSetupUtil <appHRN> <bearerToken> [outputDir] [haBaseUrl]");
            System.err.println();
            System.err.println("  appHRN      - The HRN of your app (e.g. hrn:here:account::HERE:app/abc123)");
            System.err.println("  bearerToken - A valid HERE Access Token with manage permission on the app");
            System.err.println("  outputDir   - Where to write key and credentials (default: ~/.here)");
            System.err.println("  haBaseUrl   - HERE Account base URL (default: https://account.api.here.com)");
            System.exit(1);
        }

        String appHrn = args[0];
        String bearerToken = args[1];
        String outputDir = args.length > 2 ? args[2] : DEFAULT_OUTPUT_DIR;
        String haBaseUrl = args.length > 3 ? args[3] : DEFAULT_HA_BASE_URL;

        // Extract client_id from appHRN (last segment after "app/")
        String clientId = appHrn.substring(appHrn.lastIndexOf('/') + 1);

        // 1. Generate RSA key pair
        System.out.println("Generating 2048-bit RSA key pair...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String n = base64UrlEncode(toUnsignedBytes(publicKey.getModulus()));
        String e = base64UrlEncode(toUnsignedBytes(publicKey.getPublicExponent()));

        // 2. Write private key PEM
        File outDir = new File(outputDir);
        outDir.mkdirs();

        File privateKeyFile = new File(outDir, "jwt-test-private-key.pem");
        String pem = toPemPkcs8(keyPair.getPrivate().getEncoded());
        try (FileWriter fw = new FileWriter(privateKeyFile)) {
            fw.write(pem);
        }
        privateKeyFile.setReadable(false, false);
        privateKeyFile.setReadable(true, true);
        System.out.println("Private key written to: " + privateKeyFile.getAbsolutePath());

        // 3. Write credentials properties
        File credFile = new File(outDir, "jwt-credentials.properties");
        String tokenEndpoint = haBaseUrl + "/oauth2/token";
        try (FileWriter fw = new FileWriter(credFile)) {
            fw.write("here.token.endpoint.url=" + tokenEndpoint + "\n");
            fw.write("here.auth.method=private_key_jwt\n");
            fw.write("here.client.id=" + clientId + "\n");
            fw.write("here.private.key=" + privateKeyFile.getAbsolutePath() + "\n");
            fw.write("here.key.id=" + KID + "\n");
        }
        credFile.setReadable(false, false);
        credFile.setReadable(true, true);
        System.out.println("Credentials written to: " + credFile.getAbsolutePath());

        // 4. Print JWK registration payload and curl command
        String jwkPayload = "{\n" +
                "  \"kty\": \"RSA\",\n" +
                "  \"kid\": \"" + KID + "\",\n" +
                "  \"alg\": \"RS256\",\n" +
                "  \"use\": \"sig\",\n" +
                "  \"n\": \"" + n + "\",\n" +
                "  \"e\": \"" + e + "\"\n" +
                "}";

        String appHrnEncoded = urlEncode(appHrn);

        System.out.println();
        System.out.println("=== JWK Registration Payload ===");
        System.out.println(jwkPayload);
        System.out.println();
        System.out.println("=== Register JWK via curl ===");
        String compactPayload = "{\"kty\":\"RSA\",\"kid\":\"" + KID + "\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}";
        System.out.println("curl -X POST '" + haBaseUrl + "/authentication/v1.1/apps/" + appHrnEncoded + "/jwks' \\");
        System.out.println("  -H 'Authorization: Bearer " + bearerToken + "' \\");
        System.out.println("  -H 'Content-Type: application/json' \\");
        System.out.println("  -d '" + compactPayload + "'");
        System.out.println();
        System.out.println("=== After registering, run the integration test ===");
        System.out.println("mvn test -Dtest=JwtClientAssertionIT -DjwtCredentialsFile=" + credFile.getAbsolutePath());
        System.out.println();
        System.out.println("=== To clean up later ===");
        System.out.println("curl -X DELETE '" + haBaseUrl + "/authentication/v1.1/apps/" + appHrnEncoded + "/jwks/{jwkHRN}' \\");
        System.out.println("  -H 'Authorization: Bearer <token>'");
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Convert BigInteger to unsigned byte array (strip leading zero byte if present).
     */
    private static byte[] toUnsignedBytes(BigInteger bigInt) {
        byte[] bytes = bigInt.toByteArray();
        if (bytes[0] == 0) {
            byte[] result = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, result, 0, result.length);
            return result;
        }
        return bytes;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toPemPkcs8(byte[] encoded) {
        String base64 = Base64.getEncoder().encodeToString(encoded);
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
