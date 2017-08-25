package com.here.account.oauth2.tutorial;

import java.util.Arrays;

public class Program {

    /**
     * The main method includes the bulk of the code integration,
     * for either always obtaining a fresh
     * HERE Access Token, from the HERE Account authorization server,
     * using the client_credentials grant_type or obtaining  the  Open  id
     * token.
     * @param argv the arguments to main; see usage output for details.
     */
    public static void main(String[] argv) {

        HereClientCredentialsTokenTutorial tutorial;
        if(Arrays.stream(argv).anyMatch(x -> x.toLowerCase().contains
                ("-idtoken"))) {
            tutorial = new GetHereClientCredentialsIdTokenTutorial(argv);
        } else {
            tutorial = new GetHereClientCredentialsAccessTokenTutorial(argv);
        }
        tutorial.getToken();
    }
}
