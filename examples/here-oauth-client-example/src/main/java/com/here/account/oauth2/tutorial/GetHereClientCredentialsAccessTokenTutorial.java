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
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.*;

/**
 * A tutorial class providing example code for always obtaining a fresh 
 * HERE Access Token, from the HERE Account authorization server, 
 * using the client_credentials grant_type.
 * 
 * @author kmccrack
 *
 */
public class GetHereClientCredentialsAccessTokenTutorial extends HereClientCredentialsTokenTutorial {

    public GetHereClientCredentialsAccessTokenTutorial(String[] argv) {
        super(argv);
    }

    public String getToken() {
        Args args = parseArgs(argv);
        try {
            OAuth1ClientCredentialsProvider credentials = getCredentials(args);
            TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                    ApacheHttpClientProvider.builder().build(),
                    credentials);
            Fresh<AccessTokenResponse> fresh = 
                    tokenEndpoint.requestAutoRefreshingToken(new ClientCredentialsGrantRequest());
            String accessToken = fresh.get().getAccessToken();
            if (args.isVerbose()) {
                System.out.println("HERE Access Token: " + accessToken);
            } else {
                System.out.println("HERE Access Token: " + accessToken.substring(0, 20) + "..." + accessToken.substring(accessToken.length() - 4));
            }
            return accessToken;
        } catch (Exception e) {
            System.err.println("trouble getting Here client_credentials Access Token: " + e);
            e.printStackTrace();
            exit(2);
            return null;
        }
    }
    ////////
    // print usage and exit
    ////////

    /**
     * Usage is displayed to stderr, along with exiting the process with a non-zero exit code.
     */
    protected void printUsageAndExit() {
        System.err.println("Usage: java "
                + com.here.account.oauth2.tutorial.GetHereClientCredentialsAccessTokenTutorial.class.getName()
                + " [-help]"
                + " [-v]"
                + " [path_to_credentials_property_file]");
        System.err.println("where:");
        System.err.println("  -help: means print this message and exit");
        System.err.println("  -v: sets verbose mode; WARNING: HERE Access Token will be displayed to stdout.");
        System.err.println("  path_to_credentials_property_file: optionally override the default path of ");
        System.err.println("     "+DEFAULT_CREDENTIALS_FILE_PATH+", to point to any file on your filesystem.");;
        exit(1);
    }

    ////////
    // an approach to parsing input args
    ////////
    protected Args parseArgs(String[] argv) {
        if (null == argv || argv.length > 3) {
            printUsageAndExit();
        }
        int i = 0;
        boolean verbose = false;
        String filePathString = null;
        while (i < argv.length) {
            String arg = argv[i++];
            if (arg.equals("-v")) {
                System.out.println("INFO: Running in verbose mode.");
                verbose = true;
            } else if (arg.equals("-help")) {
                System.out.println("INFO: in help mode, will print usage and exit.");
                printUsageAndExit();
            } else if (null == filePathString) {
                filePathString = arg;
            } else {
                System.err.println("unrecognized option or more than one path_to_credentials_property_file");
                printUsageAndExit();
            }
        }
        if (!verbose) {
            System.out.println("INFO: Running in quiet mode; to enable verbose mode add '-v' as your first argument.");
            System.out.println("WARNING: verbose mode will display an actual valid HERE Access Token to stdout.");
        }
        return new Args(verbose, filePathString);
    }
    
}
