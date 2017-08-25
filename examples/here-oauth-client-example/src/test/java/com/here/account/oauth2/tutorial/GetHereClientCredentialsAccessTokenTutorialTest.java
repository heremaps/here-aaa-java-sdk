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

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import org.junit.Test;
import org.mockito.Mockito;

import com.here.account.auth.OAuth1ClientCredentialsProvider;

public class GetHereClientCredentialsAccessTokenTutorialTest {

    /**
     * Build a mock HttpProvider that always returns the provided response body.
     */
    static GetHereClientCredentialsAccessTokenTutorial mockTutorial(String[] args) {
        GetHereClientCredentialsAccessTokenTutorial mock = Mockito.spy(new GetHereClientCredentialsAccessTokenTutorial(args));
        Mockito.doThrow(Helper.MyException.class).when(mock).exit(Mockito.anyInt
                ());
        return mock;
    }

    @Test(expected = Helper.MyException.class)
    public void test_help() {
        String[] args = {
                "-help"
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getToken();
    }
    
    @Test(expected = Helper.MyException.class)
    public void test_unrecognized() {
        String[] args = {
                "-unrecognized"
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getToken();
    }

    @Test(expected = Helper.MyException.class)
    public void test_null() {
        String[] args = null;
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getToken();
    }

    @Test(expected = Helper.MyException.class)
    public void test_tooManyArguments() {
        String[] args = {
                "too",
                "many",
                "arguments",
                "supplied"
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getToken();
    }

    static void setTestCreds(GetHereClientCredentialsAccessTokenTutorial tutorial,
            OAuth1ClientCredentialsProvider systemCredentials) {
        if (null == systemCredentials) {
            throw new RuntimeException("no credentials available for test");
        }
        Class<?> clazz = GetHereClientCredentialsAccessTokenTutorial.class;
        try {
            Field field = clazz.getDeclaredField("testCreds");
            field.setAccessible(true);
            field.set(tutorial, systemCredentials);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("fail to get testCreds declared field: " + e, e);
        }
    }

    @Test(expected = Helper.MyException.class)
    public void test_broken_defaultCredentialsFile() {
        File file = GetHereClientCredentialsAccessTokenTutorial.getDefaultCredentialsFile();
        String path = null != file ? file.getAbsolutePath() : "broken";
        String[] args = {
                path + UUID.randomUUID().toString()
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getToken();
    }

    /**
     * Note: we don't want to test verbose mode with a potentially-real credentials.properties file, 
     * because that would cause the real access token to go to stdout.
     */
    @Test(expected = Helper.MyException.class)
    public void test_verbose_broken_defaultCredentialsFile() {
        File file = GetHereClientCredentialsAccessTokenTutorial.getDefaultCredentialsFile();
        String path = null != file ? file.getAbsolutePath() : "broken";
        String[] args = {
                "-v",
                path + UUID.randomUUID().toString()
        };
        GetHereClientCredentialsAccessTokenTutorial tutorial = mockTutorial(args);
        tutorial.getToken();
    }
}
