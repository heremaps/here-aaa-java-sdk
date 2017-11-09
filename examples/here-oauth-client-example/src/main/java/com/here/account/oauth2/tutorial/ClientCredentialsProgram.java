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

import java.util.Arrays;

public class ClientCredentialsProgram {

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
