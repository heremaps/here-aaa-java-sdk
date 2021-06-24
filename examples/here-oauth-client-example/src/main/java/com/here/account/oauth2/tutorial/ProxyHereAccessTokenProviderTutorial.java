/*
 * Copyright (c) 2017 HERE Europe B.V.
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

import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.HereAccessTokenProvider;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * A simple tutorial demonstrating how to get a HERE Access Token over Proxy.
 *
 * @author johns
 */
public class ProxyHereAccessTokenProviderTutorial {

    public static void main(String[] argv) {
        ProxyHereAccessTokenProviderTutorial t = new ProxyHereAccessTokenProviderTutorial(argv);
        t.doGetAccessToken();
    }

    private final Args args;

    private ProxyHereAccessTokenProviderTutorial(String[] argv) {
        this.args = parseArgs(argv);
    }

    /**
     * A simple method that builds a HereAccessTokenProvider,
     * gets one Access Token,
     * and if successful outputs the first few characters of the valid token.
     */
    protected void doGetAccessToken() {

        try (

            // use your provided System properties, ~/.here/credentials.ini, or credentials.properties file
            HereAccessTokenProvider accessTokens = HereAccessTokenProvider
                .builder()
                .setHttpProvider(createHttpProvider())
                .build()
            ) {
            // call accessTokens.getAccessToken(); every time one is needed, it will always be fresh
            String accessToken = accessTokens.getAccessToken();
            // use accessToken on a request...

            useAccessToken(accessToken);
        } catch (Exception e) {
            trouble(e);
        }

    }

    private static HttpProvider createHttpProvider() {
        HttpProvider httpProvider = null;
        Proxy proxy = Proxy.createProxyFromEnvironment();
        if (proxy != null) {
            System.out.println("===> " + proxy.toString());
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                new AuthScope(proxy.getProxyHost(), proxy.getProxyPort()),
                new UsernamePasswordCredentials(proxy.getProxyUser(), proxy.getProxyPassword()));
            RequestConfig.Builder configBuilder = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .setProxy(new HttpHost(proxy.getProxyHost(), proxy.getProxyPort()));
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credsProvider)
                .setDefaultRequestConfig(configBuilder.build());

            httpProvider = ApacheHttpClientProvider.builder()
                .setHttpClient(httpClientBuilder.build())
                .setDoCloseHttpClient(true)
                .build();
        } else {
            System.out.println("===> proxy not used.");
            httpProvider = ApacheHttpClientProvider.builder().build();
        }
        return httpProvider;
    }


    protected void useAccessToken(String accessToken) {
        if (args.isVerbose()) {
            System.out.println("got HERE Access Token: " + accessToken);
        } else {
            System.out.println("got HERE Access Token: " + accessToken.substring(0, 20) + "..." + accessToken.substring(accessToken.length() - 4));
        }
    }

    protected void trouble(Exception e) {
        System.err.println("trouble " + e);
        e.printStackTrace();
        exit(1);
    }

    protected void exit(int status) {
        System.exit(status);
    }

    ////////
    // an approach to parsing input args
    ////////
    protected static class Args {
        private final boolean verbose;

        public Args(boolean verbose) {
            this.verbose = verbose;
        }

        public boolean isVerbose() {
            return verbose;
        }
    }

    protected Args parseArgs(String[] argv) {
        if (null == argv) {
            printUsageAndExit();
        }
        int i = 0;
        boolean verbose = false;
        while (i < argv.length) {
            String arg = argv[i++];
            if (arg.equals("-v")) {
                System.out.println("INFO: Running in verbose mode.");
                verbose = true;
            } else if (arg.equals("-help")) {
                System.out.println("INFO: in help mode, will print usage and exit.");
                printUsageAndExit();
            } else {
                System.err.println("unrecognized option");
                printUsageAndExit();
            }
        }
        if (!verbose) {
            System.out.println("INFO: Running in quiet mode; to enable verbose mode add '-v' as your first argument.");
            System.out.println("WARNING: verbose mode will display an actual valid HERE Access Token to stdout.");
        }
        return new Args(verbose);
    }

    ////////
    // print usage and exit
    ////////

    /**
     * Usage is displayed to stderr, along with exiting the process with a non-zero exit code.
     */
    protected void printUsageAndExit() {
        System.err.println("Usage: java "
                + ProxyHereAccessTokenProviderTutorial.class.getName()
                + " [-help]"
                + " [-v]");
        System.err.println("where:");
        System.err.println("  -help: means print this message and exit");
        System.err.println("  -v: sets verbose mode; WARNING: HERE Access Token will be displayed to stdout.");
        exit(1);
    }
}
