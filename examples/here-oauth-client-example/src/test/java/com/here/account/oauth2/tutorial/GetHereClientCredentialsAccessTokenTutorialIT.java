package com.here.account.oauth2.tutorial;

import java.io.File;

import org.junit.Test;

public class GetHereClientCredentialsAccessTokenTutorialIT {

    @Test
    public void test_explicit_defaultCredentialsFile() {
        File file = GetHereClientCredentialsAccessTokenTutorial.getDefaultCredentialsFile();
        String path = null != file ? file.getAbsolutePath() : "broken";
        String[] args = {
                path
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = 
                GetHereClientCredentialsAccessTokenTutorialTest.mockTutorial(args);
        if (null == file) {
            GetHereClientCredentialsAccessTokenTutorialTest.setTestCreds(tutorial, 
                    GetHereClientCredentialsAccessTokenTutorialTest.getSystemCredentials());
        }
        tutorial.getAccessToken();
    }
    
    @Test
    public void test_noArgs_defaultCredentialsFile() {
        File file = GetHereClientCredentialsAccessTokenTutorial.getDefaultCredentialsFile();
        String[] args = {
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = 
                GetHereClientCredentialsAccessTokenTutorialTest.mockTutorial(args);
        if (null == file) {
            GetHereClientCredentialsAccessTokenTutorialTest.setTestCreds(tutorial, 
                    GetHereClientCredentialsAccessTokenTutorialTest.getSystemCredentials());
        }
        tutorial.getAccessToken();
    }
    


}
