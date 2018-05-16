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

import com.here.account.http.HttpConstants.HttpMethods;
import org.junit.After;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * @author kmccrack
 */
public class FromHereCredentialsIniFileTest extends FromHereCredentialsIniConstants {

    File file;
    FromHereCredentialsIniFile fromFile;

    protected void createTmpFile() throws IOException {
        String prefix = UUID.randomUUID().toString();
        file = File.createTempFile(prefix, null);
        file.deleteOnExit();
    }

    protected void createTmpFileWithContent() throws IOException {
        createTmpFile();

        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write(new String(getDefaultIniStreamContents(), StandardCharsets.UTF_8));
        bw.close();
    }

    @After
    public void tearDown() {
        if (null != file) {
            file.delete();
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_null_file() {
        fromFile = new FromHereCredentialsIniFile(null, TEST_DEFAULT_INI_SECTION_NAME);
    }

    @Test(expected = RuntimeException.class)
    public void test_nonExistant_file() {
        fromFile = new FromHereCredentialsIniFile(new File(UUID.randomUUID().toString()), TEST_DEFAULT_INI_SECTION_NAME);
        fromFile.getTokenEndpointUrl();
    }

    @Test
    public void test_getDelegate() throws IOException {
        createTmpFileWithContent();

        fromFile = new FromHereCredentialsIniFile(file, TEST_DEFAULT_INI_SECTION_NAME);
        String actualTokenEndpointUrl = fromFile.getTokenEndpointUrl();
        assertTrue("tokenEndpointUrl expected "+expectedTokenEndpointUrl+", actual "+actualTokenEndpointUrl,
                expectedTokenEndpointUrl.equals(actualTokenEndpointUrl));
    }

    @Test (expected = NullPointerException.class)
    public void test_getDelegateThrowsNonIOException() throws IOException {
        createTmpFile();

        fromFile = new FromHereCredentialsIniFile(file, TEST_DEFAULT_INI_SECTION_NAME);
        String actualTokenEndpointUrl = fromFile.getTokenEndpointUrl();
    }

    @Test
    public void test_default_file() {
        fromFile = new FromHereCredentialsIniFile();
        File actualFile = fromFile.getFile();
        String actualName = actualFile.getName();
        String expectedName = "credentials.ini";
        assertTrue("default file name expected "+expectedName+", actual "+actualName, expectedName.equals(actualName));
    }


    @Test
    public void test_basic_file() throws IOException {
        createTmpFileWithContent();

        FromHereCredentialsIniStreamTest otherTezt = new FromHereCredentialsIniStreamTest();
        byte[] bytes = otherTezt.getDefaultIniStreamContents();

        try (OutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
            outputStream.flush();
        }

        // use the file
        fromFile = new FromHereCredentialsIniFile(file, TEST_DEFAULT_INI_SECTION_NAME);
        otherTezt.verifyExpected(fromFile);
    }

    @Test
    public void test_getHttpMethod() {
        fromFile = new FromHereCredentialsIniFile();
        HttpMethods httpMethod = fromFile.getHttpMethod();
        HttpMethods expectedHttpMethod = HttpMethods.POST;
        assertTrue("httpMethod expected " + expectedHttpMethod + ", actual " + httpMethod,
                expectedHttpMethod.equals(httpMethod));
    }
}
