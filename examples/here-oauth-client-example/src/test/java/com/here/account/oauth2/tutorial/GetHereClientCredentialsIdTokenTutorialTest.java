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
import org.junit.Test;
import org.junit.Assert;
import org.mockito.Mockito;

public class GetHereClientCredentialsIdTokenTutorialTest {

    static GetHereClientCredentialsIdTokenTutorial mockTutorial(String[] args) {
        GetHereClientCredentialsIdTokenTutorial mock = Mockito.spy(new
                GetHereClientCredentialsIdTokenTutorial(args));
        Mockito.doThrow(Helper.MyException.class).when(mock).exit(Mockito.anyInt
                ());
        return mock;
    }

    @Test(expected = Helper.MyException.class)
    public void test_help() {
        String[] args = {
                "-help"
        };
        GetHereClientCredentialsIdTokenTutorial tutorial = mockTutorial(args);
        tutorial.getToken();
    }

    @Test(expected = Helper.MyException.class)
    public void test_unrecognized() {
        String[] args = {
                "-unrecognized"
        };
        GetHereClientCredentialsIdTokenTutorial tutorial = mockTutorial(args);
        tutorial.getToken();
    }

    @Test(expected = Helper.MyException.class)
    public void test_null() {
        String[] args = null;
        GetHereClientCredentialsIdTokenTutorial tutorial = mockTutorial(args);
        tutorial.getToken();
    }

    @Test
    public void test_id_token()  {
        File file = GetHereClientCredentialsIdTokenTutorial.getDefaultCredentialsFile();
        String path = null != file ? file.getAbsolutePath() : "broken";
        String[] args = {
                path
        };
        GetHereClientCredentialsIdTokenTutorial tutorial = mockTutorial(args);
        if (null == file) {
            Helper.setTestCreds(tutorial, Helper.getSystemCredentials());
        }
        String idToken = tutorial.getToken();
        Assert.assertNotNull(idToken);
    }

    @Test(expected = Helper.MyException.class)
    public void test_tooManyArguments() {
        String[] args = {
                "-v",
                "-idToken",
                "-help",
                "filePath",
                "testPath"
        };
        GetHereClientCredentialsIdTokenTutorial tutorial =
                mockTutorial(args);
        tutorial.getToken();
    }
}
