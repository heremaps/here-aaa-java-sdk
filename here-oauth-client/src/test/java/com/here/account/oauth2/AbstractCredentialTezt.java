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

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.Before;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.auth.OAuth1Signer;
import com.here.account.http.HttpProvider;
import com.here.account.http.java.JavaHttpProvider;


public abstract class AbstractCredentialTezt {
    
    OAuth1ClientCredentialsProvider hereCredentialsProvider;
    
    private static final String USER_DOT_HOME = "user.home";
    private static final String DOT_HERE_SUBDIR = ".here";
    private static final String CREDENTIALS_DOT_PROPERTIES_FILENAME = "credentials.properties";
    
    protected HttpProvider getHttpProvider() {
        // default Java HttpProvider
        return JavaHttpProvider.builder().build();
    }

    protected File getDefaultCredentialsFile() {
        String userDotHome = System.getProperty(USER_DOT_HOME);
        if (userDotHome != null && userDotHome.length() > 0) {
            File dir = new File(userDotHome, DOT_HERE_SUBDIR);
            if (dir.exists() && dir.isDirectory()) {
                File file = new File(dir, CREDENTIALS_DOT_PROPERTIES_FILENAME);
                if (file.exists() && file.isFile()) {
                    return file;
                }
            }
        }
        return null;
    }
    
    protected String getDefaultCredentialsFilePathString() {
        return "~" + File.separatorChar + DOT_HERE_SUBDIR + File.separatorChar + CREDENTIALS_DOT_PROPERTIES_FILENAME;
    }
    
    protected String url;
    protected String accessKeyId;
    protected String accessKeySecret;
    
    @Before
    public void setUp() throws Exception {
        // -DhereCredentialsFile
        File file = null;
        String hereCredentialsFile = System.getProperty("hereCredentialsFile");
        if (null != hereCredentialsFile) {
            file = new File(hereCredentialsFile);
            if (!file.exists()) {
                fail("file does not exist");
            }
        }

        if (null == file) {
            // default ~/.here/credentials.properties
            file = getDefaultCredentialsFile();
        }
        if (null != file) {
            OAuth1ClientCredentialsProvider propertiesCredentialsProvider = 
                    new OAuth1ClientCredentialsProvider.FromFile(file);
            hereCredentialsProvider = propertiesCredentialsProvider;
            url = hereCredentialsProvider.getTokenEndpointUrl();
            Field field = OAuth1ClientCredentialsProvider.class.getDeclaredField("oauth1Signer");
            field.setAccessible(true);
            OAuth1Signer oauth1Signer = (OAuth1Signer) field.get(hereCredentialsProvider);
            field = OAuth1Signer.class.getDeclaredField("consumerKey");
            field.setAccessible(true);
            accessKeyId = (String) field.get(oauth1Signer);
            field = OAuth1Signer.class.getDeclaredField("consumerSecret");
            field.setAccessible(true);
            accessKeySecret = (String) field.get(oauth1Signer);
        }
        
        // jenkins CI
        String url = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY
                );
        String accessKeyId = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY);
        String accessKeySecret = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY);
        if (isNotBlank(url) && isNotBlank(accessKeyId) && isNotBlank(accessKeySecret)) {
            this.url = url;
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
            // System.properties override
            hereCredentialsProvider = new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret);
        }
        
        if (null == hereCredentialsProvider) {
            this.url = "http://mock.example.com";
            this.accessKeyId = "testAccessKeyId";
            this.accessKeySecret = "testAccessKeySecret";
            hereCredentialsProvider = new OAuth1ClientCredentialsProvider(this.url, this.accessKeyId, this.accessKeySecret);
        }
        
        // verify some credentials will be available
        assertTrue("no credentials configs were available, try populating "
                + getDefaultCredentialsFilePathString(), 
                null != hereCredentialsProvider);
    }

    protected boolean isNotBlank(String str) {
        return null != str && str.trim().length() > 0;
    }

}
