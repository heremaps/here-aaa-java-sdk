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

import com.here.account.oauth2.HereAccessTokenProvider;

/**
 * A simple tutorial demonstrating how to get a HERE Access Token.
 *
 * @author kmccrack
 */
public class HereAccessTokenProviderTutorial {

    public static void main(String[] argv) {
        HereAccessTokenProviderTutorial t = new HereAccessTokenProviderTutorial();
        t.doGetAccessToken();
    }

    private HereAccessTokenProviderTutorial() {
    }

    /**
     * A simple method that builds a HereAccessTokenProvider,
     * gets one Access Token,
     * and if successful outputs the first few characters of the valid token.
     */
    protected void doGetAccessToken() {
        try (
            HereAccessTokenProvider accessTokens = HereAccessTokenProvider.builder().build()
        ) {
            String accessToken = accessTokens.getAccessToken();
            useAccessToken(accessToken);
        } catch (Exception e) {
            trouble(e);
        }

    }

    protected void useAccessToken(String accessToken) {
        System.out.println("got access token " + accessToken.substring(0, 5) + "...");
    }

    protected void trouble(Exception e) {
        System.err.println("trouble " + e);
        e.printStackTrace();
        exit(1);
    }

    protected void exit(int status) {
        System.exit(status);
    }

}
