
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
package com.here.account.oauth2;

import static org.junit.Assert.assertTrue;

import com.here.account.util.JacksonSerializer;
import com.here.account.util.Serializer;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileAccessTokenResponseTest {

    private FileAccessTokenResponse response;

    private File tmpFile;
    private Serializer serializer;

    private long prevStartTimeMillis;
    private long prevExp;
    private long prevExpiresIn;

    @Test
    public void test_backwardcompatible_6_arg_constructor_noscope() {
        String accessToken = "accessToken";
        String tokenType = "bearer";
        Long expiresIn = 123L;
        String refreshToken = "refreshToken";
        String idToken = "idToken";
        Long exp = expiresIn + (System.currentTimeMillis() / 1000L);

        response = new FileAccessTokenResponse(accessToken, tokenType, expiresIn, refreshToken, idToken, exp);

        String actualAccessToken = response.getAccessToken();
        assertTrue("expected accessToken " + accessToken + ", actual " + actualAccessToken,
                accessToken.equals(actualAccessToken));
        String actualTokenType = response.getTokenType();
        assertTrue("expected tokenType " + tokenType + ", actual " + actualTokenType,
                tokenType.equals(actualTokenType));
        Long actualExpiresIn = response.getExpiresIn();
        long high = expiresIn;
        long low = expiresIn - 5;
        assertTrue("expected expiresIn " + expiresIn + ", actual " + actualExpiresIn,
                low <= actualExpiresIn && actualExpiresIn <= high);
        String actualRefreshToken = response.getRefreshToken();
        assertTrue("expected refreshToken " + refreshToken + ", actual " + actualRefreshToken,
                refreshToken.equals(actualRefreshToken));
        String actualIdToken = response.getIdToken();
        assertTrue("expected idToken " + idToken + ", actual " + actualIdToken,
                idToken.equals(actualIdToken));
        Long actualExp = response.getExp();
        assertTrue("expected exp " +  exp + ", actual " + actualExp,
                exp.equals(actualExp));
        String actualScope = response.getScope();
        assertTrue("expected scope null, actual " + actualScope,
                null == actualScope);

    }

    @Test
    public void test_expiresIn() {
        String accessToken = "my-access-token";
        String tokenType = null;
        Long expiresIn = null;
        String refreshToken = null;
        String idToken = null;
        int secondsFromNow = 45;
        Long exp = (System.currentTimeMillis() / 1000L) + secondsFromNow;
        String expectedScope = "my-scope";
        
        response = new FileAccessTokenResponse( accessToken, 
                 tokenType,
                 expiresIn,  refreshToken,  idToken,
                 exp, expectedScope);
        
        String actualAccessToken = response.getAccessToken();
        assertTrue("accessToken didn't match expected "+accessToken+", actual "+actualAccessToken,
                accessToken.equals(actualAccessToken));
        
        int minExpiresIn = secondsFromNow - 5;
        Long actualExpiresIn = response.getExpiresIn();
        int maxExpiresIn = secondsFromNow + 5;
        assertTrue("expected expiresIn between " + minExpiresIn + " and " + maxExpiresIn + ", but got " 
                + actualExpiresIn, 
                null != actualExpiresIn && minExpiresIn <= actualExpiresIn && actualExpiresIn <= maxExpiresIn);

        Long actualExp = response.getExp();
        assertTrue("expected exp " + exp + ", actual " + actualExp,
                exp.equals(actualExp));

        String actualScope = response.getScope();
        assertTrue("expected scope " + expectedScope + ", actual " + actualScope,
                expectedScope.equals(actualScope));
    }

    /**
     * Tests the timestamp and relative timings for a FileAccessTokenResponse,
     * when read multiple times from the same previously-serialized file,
     * over increasing clock time.
     *
     * @throws IOException if IO trouble
     * @throws InterruptedException if interrupted during sleep
     */
    @Test
    public void test_timings_sameFile_multipleReads() throws IOException, InterruptedException {
        String accessToken = "my-access-token";
        String tokenType = "bearer";
        Long expiresIn = 45L;
        String refreshToken = null;
        String idToken = null;

        Long exp = (System.currentTimeMillis() / 1000L) + expiresIn;
        String expectedScope = null;

        response = new FileAccessTokenResponse(accessToken,
                tokenType,
                expiresIn, refreshToken, idToken,
                exp, expectedScope);

        long startTimeMillis = response.getStartTimeMilliseconds();

        tmpFile = File.createTempFile("access_token", ".json");
        tmpFile.deleteOnExit();

        serializer = new JacksonSerializer();
        try (OutputStream outputStream = new FileOutputStream(tmpFile)) {
            serializer.writeObjectToJson(outputStream, response);
        }

        prevStartTimeMillis = startTimeMillis;
        prevExp = exp;
        prevExpiresIn = expiresIn;
        for (int i = 0; i < 3; i++) {
            verifyTimings();
        }
    }

    private void verifyTimings() throws InterruptedException, IOException {
        Thread.sleep(1001L);
        Path path = Paths.get(tmpFile.toURI());
        try (
                InputStream in = Files.newInputStream(path)) {
            FileAccessTokenResponse fileAccessTokenResponse = serializer.jsonToPojo(
                    in,
                    FileAccessTokenResponse.class
            );
            long startTimeMillis2 = fileAccessTokenResponse.getStartTimeMilliseconds();
            long exp2 = fileAccessTokenResponse.getExp();
            long expiresIn2 = fileAccessTokenResponse.getExpiresIn();

            long low = prevStartTimeMillis + 1000;
            long high = prevStartTimeMillis + 3000;

            assertTrue("startTimeMillis was expected more than " + prevStartTimeMillis
                            + ", actual " + startTimeMillis2 + " didn't fall in range (" + low + ", " + high + ")",
                    low < startTimeMillis2 && startTimeMillis2 < prevStartTimeMillis + 2000);
            assertTrue("exp was expected " + prevExp + ", actual " + exp2,
                    prevExp == exp2);
             low = prevExpiresIn - 3;
             high = prevExpiresIn;
            assertTrue("expiresIn was expected less than " + prevExpiresIn
                            + ", actual " + expiresIn2 + " didn't fall in range (" + low + ", " + high + ")",
                    low < expiresIn2 && expiresIn2 < high);

            prevStartTimeMillis = startTimeMillis2;
            prevExp = exp2;
            prevExpiresIn = expiresIn2;
        }
    }

}