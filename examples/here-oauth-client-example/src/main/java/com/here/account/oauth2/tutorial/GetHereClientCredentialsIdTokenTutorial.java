package com.here.account.oauth2.tutorial;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.*;

public class GetHereClientCredentialsIdTokenTutorial extends HereClientCredentialsTokenTutorial {

    public GetHereClientCredentialsIdTokenTutorial(String[] argv) {
        super(argv);
    }

    /**
     * Get Access token and Open Id by setting the  scope in the request
     */
    public String getToken() {
        Args args = parseArgs(argv);
        try {
            OAuth1ClientCredentialsProvider credentials = getCredentials(args);
            TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                    ApacheHttpClientProvider.builder().build(),
                    credentials);
            AccessTokenRequest accessTokenRequest  =  new
                    ClientCredentialsGrantRequest();
            accessTokenRequest.setScope("openid");
            AccessTokenResponse token =
                    tokenEndpoint.requestToken(accessTokenRequest);
            String idToken = token.getIdToken();
            if (args.isVerbose()) {
                System.out.println("Id Token: " + idToken);
            } else {
                System.out.println("Id Token: " + idToken.substring(0, 20)
                        + "..." + idToken.substring(idToken.length() - 4));
            }
            return idToken;
        } catch (Exception e) {
            System.err.println("trouble getting Here client_credentials Id " +
                    "Token: " + e);
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
                + com.here.account.oauth2.tutorial.GetHereClientCredentialsIdTokenTutorial.class.getName()
                + " [-idToken]"
                + " [-help]"
                + " [-v]"
                + " [path_to_credentials_property_file]");
        System.err.println("where:");
        System.err.println("  -idToken: means get the id token (open id)");
        System.err.println("  -help: means print this message and exit");
        System.err.println("  -v: sets verbose mode; WARNING: HERE Id Token " +
                "will be displayed to stdout.");
        System.err.println("  path_to_credentials_property_file: optionally override the default path of ");
        System.err.println("     "+DEFAULT_CREDENTIALS_FILE_PATH+", to point to any file on your filesystem.");;
        exit(1);
    }

    ////////
    // an approach to parsing input args
    ////////
    protected Args parseArgs(String[] argv) {
        if (null == argv || argv.length > 4) {
            printUsageAndExit();
        }
        int i = 0;
        boolean verbose = false;
        String filePathString = null;
        while (i < argv.length) {
            String arg = argv[i++];
            if(arg.toLowerCase().equals("-idtoken")) {
                continue;
            }
            if (arg.equals("-v")) {
                System.out.println("INFO: Running in verbose mode.");
                verbose = true;
            } else if (arg.equals("-help")) {
                System.out.println("INFO: in help mode, will print usage and exit.");
                printUsageAndExit();
            } else if(null == filePathString) {
                filePathString = arg;
            } else {
                System.out.println("unrecognized option or more than one path_to_credentials_property_file");
                printUsageAndExit();
            }
        }
        if (!verbose) {
            System.out.println("INFO: Running in quiet mode; to enable verbose mode add '-v' as your first argument.");
            System.out.println("WARNING: verbose mode will display an actual " +
                    "valid HERE Id Token to stdout.");
        }
        return new Args(verbose, filePathString);
    }
}