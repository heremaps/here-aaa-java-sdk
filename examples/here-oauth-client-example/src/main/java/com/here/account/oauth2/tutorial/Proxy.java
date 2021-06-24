/*
 * Copyright (C) 2017-2018 HERE Europe B.V.
 * SPDX‐License‐Identifier: MIT
 */

package com.here.account.oauth2.tutorial;

/**
 * A class to store proxy properties.<p>
 * Utility function provided to parse environment variables HTTPS_PROXY or HTTP_PROXY.<p>
 * Expected formats: <br>
 *      http://user:password@server1:3433<br>
 *      https://user:password@server1:3433
 */
public class Proxy {

    private static final String HTTP_PROXY   = "HTTP_PROXY";
    private static final String HTTPS_PROXY  = "HTTPS_PROXY";

    private final String        proxyHost;
    private final int           proxyPort;
    private final String        proxyUser;
    private final String        proxyPassword;

    public Proxy(String proxyHost, int proxyPort, String proxyUser, String proxyPassword) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public static Proxy createProxyFromEnvironment() {

        String envProxy = null;
        if (System.getenv(HTTPS_PROXY) != null) {
            envProxy = System.getenv(HTTPS_PROXY);
        }
        if (envProxy == null && System.getenv(HTTPS_PROXY.toLowerCase()) != null) {
            envProxy = System.getenv(HTTPS_PROXY.toLowerCase());
        }
        if (envProxy == null && System.getenv(HTTP_PROXY) != null) {
            envProxy = System.getenv(HTTP_PROXY);
        }
        if (envProxy == null && System.getenv(HTTP_PROXY.toLowerCase()) != null) {
            envProxy = System.getenv(HTTP_PROXY.toLowerCase());
        }
        if (envProxy == null) {
            return null;
        }

        String[] tokens = envProxy.trim().split(":|@|//");
        String user   = tokens[2];
        String secret = tokens[3];
        String host   = tokens[4];
        int port      = Integer.parseInt(tokens[5]);

        return new Proxy(host, port, user, secret);
    }

    @Override
    public String toString() {
        return "Proxy{" +
                "proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                ", proxyUser='" + proxyUser + '\'' +
                ", proxyPassword='" + proxyPassword + '\'' +
                '}';
    }

}
