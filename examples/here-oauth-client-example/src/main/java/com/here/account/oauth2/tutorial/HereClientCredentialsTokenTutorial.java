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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;

    public class HereClientCredentialsTokenTutorial {

        protected String[] argv;
        private OAuth1ClientCredentialsProvider testCreds = null;

        public HereClientCredentialsTokenTutorial(String[] argv) {
            this.argv = argv;
        }

        protected OAuth1ClientCredentialsProvider getCredentials(com.here.account.oauth2.tutorial.GetHereClientCredentialsAccessTokenTutorial.Args args) throws IOException {
            if (null != testCreds) {
                return testCreds;
            }

            File file = getCredentialsFile(args);
            return new OAuth1ClientCredentialsProvider.FromFile(file);
        }

        protected void exit(int status) {
            System.exit(status);
        }

        protected class Args {
            private final boolean verbose;
            private final String filePathString;

            public Args(boolean verbose, String filePathString) {
                this.verbose = verbose;
                this.filePathString = filePathString;
            }

            public boolean isVerbose() {
                return verbose;
            }

            public String getFilePathString() {
                return filePathString;
            }

        }

        public String getToken() {
            throw new NotImplementedException();
        }

        protected  void printUsageAndExit() {
            throw new NotImplementedException();
        }
        ////////
        // get credentials file, either command-line override, or default file location
        ////////

        protected File getCredentialsFile(com.here.account.oauth2.tutorial.GetHereClientCredentialsAccessTokenTutorial.Args args) {
            File file;
            String filePathString = args.getFilePathString();
            if (null != filePathString) {
                file = new File(filePathString);
                if (!isFileAndExists(file)) {
                    System.err.println("WARNING: credentials properties file does not exist: " + file);
                    printUsageAndExit();
                }
            } else {
                file = getDefaultCredentialsFile();
                if (null == file) {
                    System.err.println("WARNING: " + DEFAULT_CREDENTIALS_FILE_PATH
                            + " default credentials file location does not exist, please specify a location");
                    printUsageAndExit();
                }
            }
            return file;
        }

        ////////
        // a possible default path to credentials properties file
        ////////

        protected static final String USER_DOT_HOME = "user.home";
        protected static final String DOT_HERE_SUBDIR = ".here";
        protected static final String CREDENTIALS_DOT_PROPERTIES_FILENAME =
                "credentials.properties";
        protected static final String DEFAULT_CREDENTIALS_FILE_PATH = "~" + File
                .separatorChar + DOT_HERE_SUBDIR
                + File.separatorChar + CREDENTIALS_DOT_PROPERTIES_FILENAME;

        protected static File getDefaultCredentialsFile() {
            String userDotHome = System.getProperty(USER_DOT_HOME);
            if (userDotHome != null && userDotHome.length() > 0) {
                File dir = new File(userDotHome, DOT_HERE_SUBDIR);
                if (dir.exists() && dir.isDirectory()) {
                    File file = new File(dir, CREDENTIALS_DOT_PROPERTIES_FILENAME);
                    if (isFileAndExists(file)) {
                        return file;
                    }
                }
            }
            return null;
        }

        protected static boolean isFileAndExists(File file) {
            return file.exists() && file.isFile();
        }

    }
