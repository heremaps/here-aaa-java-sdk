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

import com.here.account.auth.OAuth1ClientCredentialsProvider;

import java.lang.reflect.Field;

public class Helper {
    public static class MyException extends Exception {
        public MyException() {
            super("in tests, this is used to prevent System.exit(..) from running");
        }
    }

    public static void setTestCreds(HereClientCredentialsTokenTutorial tutorial,
                                    OAuth1ClientCredentialsProvider systemCredentials) {
        if (null == systemCredentials) {
            throw new RuntimeException("no credentials available for test");
        }
        Class<?> clazz = HereClientCredentialsTokenTutorial.class;
        try {
            Field field = clazz.getDeclaredField("testCreds");
            field.setAccessible(true);
            field.set(tutorial, systemCredentials);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("fail to get testCreds declared field: " + e, e);
        }
    }

    public static OAuth1ClientCredentialsProvider getSystemCredentials() {
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

    static boolean isNotBlank(String str) {
        return null != str && str.trim().length() > 0;
    }
}
