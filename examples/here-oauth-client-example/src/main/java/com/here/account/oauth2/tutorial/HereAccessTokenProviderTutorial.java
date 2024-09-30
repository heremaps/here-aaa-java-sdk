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

import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.HereAccessTokenProvider;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.proxy.auth.AuthType;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

/**
 * A simple tutorial demonstrating how to get a HERE Access Token.
 *
 * @author kmccrack
 */
public class HereAccessTokenProviderTutorial {

    public static void main(String[] argv) {
        HereAccessTokenProviderTutorial t = new HereAccessTokenProviderTutorial(argv);
        t.doGetAccessToken();
        t.doGetAccessTokenViaProxy();
        t.doGetAccessTokenViaProxyAndAuth();
    }
    
    private final Args args;
    
    private HereAccessTokenProviderTutorial(String[] argv) {
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
            HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder().build()
            ) {
            // call accessTokens.getAccessToken(); every time one is needed, it will always be fresh
            String accessToken = accessTokens.getAccessToken();
            // use accessToken on a request...
            
            useAccessToken(accessToken);
        } catch (Exception e) {
            trouble(e);
        }

    }

    /**
     * A simple method that builds a HereAccessTokenProvider via a proxy,
     * gets one Access Token,
     * and if successful outputs the first few characters of the valid token.
     */
    protected void doGetAccessTokenViaProxy() {
        try {
            // Creating custom httpClient for test purposes as real server will require SSL verification certificates
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            // Create a proxy
            BrowserMobProxy proxy = new BrowserMobProxyServer();
            // Start proxy
            proxy.start(0);
            try (
                    // use your provided System properties, ~/.here/credentials.ini, or credentials.properties file
                    HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                            .setHttpProvider(ApacheHttpClientProvider.builder().setHttpClient(httpClient).build())
                            .setProxy("localhost", proxy.getPort(), "http")
                            .build()
            ) {
                // call accessTokens.getAccessToken(); every time one is needed, it will always be fresh
                String accessToken = accessTokens.getAccessToken();
                // use accessToken on a request...
                useAccessToken(accessToken);
            } catch (Exception e) {
                trouble(e);
            }
            finally {
                // Stop proxy
                proxy.stop();
            }
        } catch (Exception e) {
            trouble(e);
        }
    }

    /**
     * A simple method that builds a HereAccessTokenProvider via a proxy and Auth,
     * gets one Access Token,
     * and if successful outputs the first few characters of the valid token.
     */
    protected void doGetAccessTokenViaProxyAndAuth() {
        try {
            // Creating custom httpClient for test purposes as real server will require SSL verification certificates
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            // Create a proxy
            BrowserMobProxy proxy = new BrowserMobProxyServer();
            // Start proxy
            proxy.start(0);
            String proxyUsername = "yourUsername";
            String proxyPassword = "yourPassword";
            proxy.autoAuthorization("localhost:" + proxy.getPort(), proxyUsername, proxyPassword, AuthType.BASIC);
            try (
                    // use your provided System properties, ~/.here/credentials.ini, or credentials.properties file
                    HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder()
                            .setHttpProvider(ApacheHttpClientProvider.builder().setHttpClient(httpClient).build())
                            .setProxy("localhost", proxy.getPort()) // uses https as default scheme
                            .setProxyAuthentication(proxyUsername, proxyPassword)
                            .build()
            ) {
                // call accessTokens.getAccessToken(); every time one is needed, it will always be fresh
                String accessToken = accessTokens.getAccessToken();
                // use accessToken on a request...
                useAccessToken(accessToken);
            } catch (Exception e) {
                trouble(e);
            } finally {
                // Stop proxy
                proxy.stop();
            }
        } catch (Exception e) {
            trouble(e);
        }
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
                + com.here.account.oauth2.tutorial.HereAccessTokenProviderTutorial.class.getName()
                + " [-help]"
                + " [-v]");
        System.err.println("where:");
        System.err.println("  -help: means print this message and exit");
        System.err.println("  -v: sets verbose mode; WARNING: HERE Access Token will be displayed to stdout.");
        exit(1);
    }

}
