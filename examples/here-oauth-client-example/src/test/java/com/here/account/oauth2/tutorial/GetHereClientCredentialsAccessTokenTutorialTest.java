package com.here.account.oauth2.tutorial;

import java.util.UUID;

import org.junit.Test;
import org.mockito.Mockito;

public class GetHereClientCredentialsAccessTokenTutorialTest {
    
    private static class MyException extends Exception {
        public MyException() {
            super();
        }
    }
    
    /**
     * Build a mock HttpProvider that always returns the provided response body.
     */
    private GetHereClientCredentialsAccessTokenTutorial mockTutorial(String[] args) {
        GetHereClientCredentialsAccessTokenTutorial mock = Mockito.spy(new GetHereClientCredentialsAccessTokenTutorial(args));
        Mockito.doThrow(MyException.class).when(mock).exit(Mockito.anyInt());
        return mock;
    }

    @Test
    public void test_noArgs_defaultCredentialsFile() {
        String[] args = {
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }
    
    @Test(expected = MyException.class)
    public void test_help() {
        String[] args = {
                "-help"
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }
    
    @Test(expected = MyException.class)
    public void test_unrecognized() {
        String[] args = {
                "-unrecognized"
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }

    @Test(expected = MyException.class)
    public void test_null() {
        String[] args = null;
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }

    @Test(expected = MyException.class)
    public void test_tooManyArguments() {
        String[] args = {
                "too",
                "many",
                "arguments",
                "supplied"
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }


    @Test
    public void test_explicit_defaultCredentialsFile() {
        String[] args = {
                GetHereClientCredentialsAccessTokenTutorial.getDefaultCredentialsFile().getAbsolutePath()
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }
    
    
    @Test(expected = MyException.class)
    public void test_broken_defaultCredentialsFile() {
        String[] args = {
                GetHereClientCredentialsAccessTokenTutorial.getDefaultCredentialsFile().getAbsolutePath() + UUID.randomUUID().toString()
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }

    /**
     * Note: we don't want to test verbose mode with a potentially-real credentials.properties file, 
     * because that would cause the real access token to go to stdout.
     */
    @Test(expected = MyException.class)
    public void test_verbose_broken_defaultCredentialsFile() {
        String[] args = {
                "-v",
                GetHereClientCredentialsAccessTokenTutorial.getDefaultCredentialsFile().getAbsolutePath() + UUID.randomUUID().toString()
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getAccessToken();
    }

}
