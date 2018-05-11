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

import java.nio.charset.StandardCharsets;

public abstract class FromHereCredentialsIniConstants {
    protected FromHereCredentialsIniConstants() {}

    // the constants are copied in this file to represent files that have already been issued
    // as canary tests to defend against unexpected changes in the code respect to file formats.
    protected static final String TEST_DEFAULT_INI_SECTION_NAME = "default";
    protected static final String TEST_TOKEN_ENDPOINT_URL_PROPERTY = "here.token.endpoint.url";
    protected static final String TEST_ACCESS_KEY_ID_PROPERTY = "here.access.key.id";
    protected static final String TEST_ACCESS_KEY_SECRET_PROPERTY = "here.access.key.secret";

    protected static final String SECTION_START = "[";
    protected static final String SECTION_END = "]";
    protected static final char NEWLINE = '\n';
    protected static final char EQUALS = '=';

    protected String tokenEndpointUrl = "tokenEndpointUrl";
    protected String expectedTokenEndpointUrl = tokenEndpointUrl;
    protected String accessKeyId = "accessKeyId";
    protected String accessKeySecret = "accessKeySecret";

    protected byte[] getDefaultIniStreamContents() {
        StringBuilder buf = new StringBuilder()
                .append(SECTION_START)
                .append(TEST_DEFAULT_INI_SECTION_NAME)
                .append(SECTION_END)
                .append(NEWLINE)

                .append(TEST_TOKEN_ENDPOINT_URL_PROPERTY)
                .append(EQUALS)
                .append(tokenEndpointUrl)
                .append(NEWLINE)

                .append(TEST_ACCESS_KEY_ID_PROPERTY)
                .append(EQUALS)
                .append(accessKeyId)
                .append(NEWLINE)

                .append(TEST_ACCESS_KEY_SECRET_PROPERTY)
                .append(EQUALS)
                .append(accessKeySecret)
                .append(NEWLINE)
                ;

        return buf.toString().getBytes(StandardCharsets.UTF_8);
    }
}
