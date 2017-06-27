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
package com.here.account.oauth2.tutorial;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import org.junit.Test;
import org.mockito.Mockito;

import com.here.account.auth.OAuth1ClientCredentialsProvider;

public class GetHereClientCredentialsAccessTokenTutorialTest {
    
    private static class MyException extends Exception {
        public MyException() {
            super("in tests, this is used to prevent System.exit(..) from running");
        }
    }
    
    static boolean isNotBlank(String str) {
        return null != str && str.trim().length() > 0;
    }
    
    static OAuth1ClientCredentialsProvider getSystemCredentials() {
        OAuth1ClientCredentialsProvider credentials = null;
        String url = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.TOKEN_ENDPOINT_URL_PROPERTY
                );
        String accessKeyId = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_ID_PROPERTY);
        String accessKeySecret = System.getProperty(OAuth1ClientCredentialsProvider.FromProperties.ACCESS_KEY_SECRET_PROPERTY);
        if (isNotBlank(url) && isNotBlank(accessKeyId) && isNotBlank(accessKeySecret)) {
            // System.properties override
            credentials = new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret);
        }
        return credentials;
    }
    
    /**
     * Build a mock HttpProvider that always returns the provided response body.
     */
    static GetHereClientCredentialsAccessTokenTutorial mockTutorial(String[] args) {
        GetHereClientCredentialsAccessTokenTutorial mock = Mockito.spy(new GetHereClientCredentialsAccessTokenTutorial(args));
        Mockito.doThrow(MyException.class).when(mock).exit(Mockito.anyInt());
        return mock;
    }

    @Test(expected = MyException.class)
    public void test_help() {
        String[] args = {
                "-help"
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }
    
    @Test(expected = MyException.class)
    public void test_unrecognized() {
        String[] args = {
                "-unrecognized"
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }

    @Test(expected = MyException.class)
    public void test_null() {
        String[] args = null;
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }

    @Test(expected = MyException.class)
    public void test_tooManyArguments() {
        String[] args = {
                "too",
                "many",
                "arguments",
                "supplied"
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }

    static void setTestCreds(GetHereClientCredentialsAccessTokenTutorial tutorial,
            OAuth1ClientCredentialsProvider systemCredentials) {
        if (null == systemCredentials) {
            throw new RuntimeException("no credentials available for test");
        }
        Class<?> clazz = GetHereClientCredentialsAccessTokenTutorial.class;
        try {
            Field field = clazz.getDeclaredField("testCreds");
            field.setAccessible(true);
            field.set(tutorial, systemCredentials);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("fail to get testCreds declared field: " + e, e);
        }
    }

    @Test(expected = MyException.class)
    public void test_broken_defaultCredentialsFile() {
        File file = GetHereClientCredentialsAccessTokenTutorial.getDefaultCredentialsFile();
        String path = null != file ? file.getAbsolutePath() : "broken";
        String[] args = {
                path + UUID.randomUUID().toString()
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }

    /**
     * Note: we don't want to test verbose mode with a potentially-real credentials.properties file, 
     * because that would cause the real access token to go to stdout.
     */
    @Test(expected = MyException.class)
    public void test_verbose_broken_defaultCredentialsFile() {
        File file = GetHereClientCredentialsAccessTokenTutorial.getDefaultCredentialsFile();
        String path = null != file ? file.getAbsolutePath() : "broken";
        String[] args = {
                "-v",
                path + UUID.randomUUID().toString()
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }

}
