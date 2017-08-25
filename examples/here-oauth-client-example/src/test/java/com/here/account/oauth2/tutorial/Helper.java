package com.here.account.oauth2.tutorial;

import com.here.account.auth.OAuth1ClientCredentialsProvider;

public class Helper {
    public static class MyException extends Exception {
        public MyException() {
            super("in tests, this is used to prevent System.exit(..) from running");
        }
    }
        static boolean isNotBlank(String str) {
            return null != str && str.trim().length() > 0;
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
}
