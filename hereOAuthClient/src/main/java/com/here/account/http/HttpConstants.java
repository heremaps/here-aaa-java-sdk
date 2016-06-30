package com.here.account.http;

import java.nio.charset.Charset;

public class HttpConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final Charset ENCODING_CHARSET = Charset.forName("UTF-8");
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";

    public static final int DEFAULT_REQUEST_TIMEOUT_IN_MS = 5000;
    public static final int DEFAULT_CONNECTION_TIMEOUT_IN_MS = 5000;

}
