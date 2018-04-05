/*
 * Copyright (c) 2016 HERE Europe B.V.
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
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.http.java.JavaHttpProvider;

public class JavadocsTest {
    
    /**
     * We expect FileNotFoundException because we expect the current working directory 
     * not to contain credentials.properties.
     * Clients are free to put their "credentials.properties" File anywhere on their filesystem, 
     * for positive outcomes.
     * This test verifies the Javadoc sample compiles; 
     * it would not throw any Exception if the File existed.
     * 
     * @throws IOException
     */
    // Get configuration from properties file:
    @Test(expected=FileNotFoundException.class) 
    @SuppressWarnings("unused") // code snippet from Javadocs verbatim; intentionally has unused variable
    public void test_credentialsPropertiesFile_javadocs() throws IOException {
        // setup url, accessKeyId, and accessKeySecret as properties in credentials.properties
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider.FromFile(new File("credentials.properties")));
        // choose 
        //   tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
        // or 
        //   tokenEndpoint.requestAutoRefreshingToken(new ClientCredentialsGrantRequest());
    }
    
    // Another example HttpProvider from this project below uses pure-Java.
    @Test
    @SuppressWarnings("unused") // code snippet from Javadocs verbatim; intentionally has unused variable
    public void test_JavaHttpProvider_javadocs() throws IOException {
        // create a Java HttpProvider
        HttpProvider httpProvider = JavaHttpProvider.builder().build();
        // use httpProvider
    }
    
}
