HERE Authentication, Authorization, and Accounting

Introduction
============
This repository contains the complete source code for the here-aaa-sdk project.  Basic
technical information is contained in this file.

This project is maintained by the HERE Authentication, Authorization, and Accounting team.  For
questions contact HERE_ACCOUNT_SUPPORT@here.com.

Deliverables
============
The here-aaa-sdk project produces artifacts distributed in two ways:
* The `here-oauth-client-dist-<version>.tar.gz` bundle.  External developers currently
receive this bundle as part of onboarding.
* via the [Maven Central Repository](https://search.maven.org/)

The following artifacts are published to Maven Central:

HERE OAuth Client
------
Contains code to assist developers to obtain authorization from the HERE OAuth2.0 Authorization
Server, for use with HERE Services.
```
<dependency>
  <groupId>com.here.account</groupId>
  <artifactId>here-oauth-client</artifactId>
  <version>0.4.25</version>
</dependency>
```
HERE OAuth Client Examples
------
Example usage of the HERE OAuth Client library; these are tutorials intended to be adapted into or
inform design of applications
```
<dependency>
  <groupId>com.here.account</groupId>
  <artifactId>here-oauth-client-example</artifactId>
  <version>0.4.25</version>
</dependency>
```


Directory Layout
================
Here is an overview of the top-level files contained in the repository:

    |
    +- here-oauth-client      # Source and test code for supported HERE OAuth2.0 flows
    |  |
    |  +- src                 # Source and test code
    |     |
    |     +- main             # Source code.  The generated JAR file and javadocs are delivered to developers
    |     |
    |     +- test             # Test code
    |
    +- examples               # Examples across all projects; these are tutorials intended to be adapted into or inform design of applications
    |  |
    |  +- here-oauth-client-example # Tutorial example for here-oauth-client JAR
    |     |
    |     +- src              # Source and test code
    |        |
    |        +- main          # Source code for the tutorial example
    |
    +- here-oauth-client-dist # Descriptions of how to build the .tar.gz distribution bundle

Functionality
=============
The purpose of here-oauth-client JAR is to obtain authorization from the HERE OAuth2.0
Authorization Server, for use with HERE Services.  See also https://tools.ietf.org/html/rfc6749.

The HERE Access Tokens obtained are provided as Authorization: Bearer values on requests to
HERE Services.  See also https://tools.ietf.org/html/rfc6750#section-2.1.

The here-oauth-client JAR includes
- Authentication features for signing requests to the HERE OAuth2.0 Authorization Server.  The
  client provides its provisioned id and secret to make authenticated requests via the OAuth1.0
  authentication method.
- Authorization features for obtaining HERE Access Tokens from the HERE OAuth2.0 Authorization
  Server, including the ability to automatically refresh HERE Access Tokens.  Supported
  flows include OAuth2.0 client_credentials grant for confidential clients.
- Authorization features for using OAuth2.0 Bearer HERE Access Token in the Authorization header
  for requests to HERE Services.
- Accounting claims in the Access Tokens it uses.  HERE Services extract signed Accounting claims
  from the Access Tokens.

For help, contact HERE_ACCOUNT_SUPPORT@here.com.
Built using Apache Maven (https://maven.apache.org/)

Development Setup
=================

Prerequisites
-------------

1. Requires Java 1.8.
2. Requires Apache Maven 3.3.

Build instructions
------------------

Open a command prompt at the working tree's root directory and type:

    $ mvn -DskipTests clean package

To build the package without testing it.

Test instructions
-----------------

The tests must be configured with valid HERE client credentials to pass.  To get HERE client
credentials, please contact HERE_ACCOUNT_SUPPORT@here.com.

Open a command prompt at the working tree's root directory and type:

    $ mvn clean package

Which will succeed if your client credentials file is at ~/.here/credentials.properties, and
fail the test phase otherwise.  Another way to get passing tests, or to override your
~/.here/credentials.properties, you can optionally use the command-line arguments.

Open a command prompt at the working tree's root directory and type:

    $ mvn -DargLine='-DhereCredentialsFile=/path/to/your/creds' clean package

Substitute your /path/to/your/creds above, to achieve success.

Examples instructions
---------------------
The examples directory contains a tutorial example.  To run it

1. Download and place your HERE Account authorization server credentials.properties file to
   ~/.here/credentials.properties.
2.

     $ chmod 400 ~/.here/credentials.properties
3.

     $ java -jar examples/here-oauth-client-example/target/here-oauth-client-example-*[!javadoc][!sources].jar

This tutorial uses the recommended "always fresh" approach with the default ClientAuthorizationProviderChain.
The tutorial will obtain a valid HERE Access Token and print portions of it to stdout.
If in a secure location, optionally re-run with

     $ java -jar examples/here-oauth-client-example/target/here-oauth-client-example-*[!javadoc][!sources].jar -v

to print a full valid HERE Access Token to stdout. You can also put the file in a different
location or give it a different name, just supply the file as input to the executable jar command
line.  The examples are for tutorial purposes only and MUST NOT be used in your deployed
application.  You might find it useful to start from the main(..) method's sample code, and
adapt the integration to your environment.

You can use the `-idToken` option to output the HERE Id Token (in Open ID format) instead of the 
HERE Access Token.

     $ java -cp examples/here-oauth-client-example/target/here-oauth-client-example-*[!javadoc][!sources].jar com.here.account.oauth2.tutorial.ClientCredentialsProgram -idToken

If in a secure location, optionally add the `-v` option to print a full valid Id Token to stdout.

     $ java -cp examples/here-oauth-client-example/target/here-oauth-client-example-*[!javadoc][!sources].jar com.here.account.oauth2.tutorial.ClientCredentialsProgram -idToken -v

Developer Usage
===============

Read the javadocs for details and helpful code snippets (such as setting the HTTP connection pool size).  The mvn commands 
above will create javadocs locally, which you can see at 'here-oauth-client/target/apidocs/index.html'.

If you are just getting started, go to `com.here.account.oauth2.HereAccessTokenProvider` javadocs for 
the overview of two options:
- To get a supplier of HERE Access Tokens optimized for making repeated API calls to resource servers, use `HereAccessTokenProvider.builder().build();` once followed by repeated calls to `.getAccessToken();`. This option is also recommended for long-running scenarios. In this default option, created Access Tokens are reused during their lifetime and automatically updated for you when needed.
- To get a new HERE Access Token only once, or if you want to manage your own token expirations, use `HereAccessTokenProvider.builder().setAlwaysRequestNewToken(true).build();` followed by calls to `.getAccessToken();`.

A third option is to get an id_token
- get Id Token via `com.here.account.oauth2.HereAccount`'s `TokenEndpoint.requestToken(..)` approach by setting the
scope field in the request.

# License

Copyright (C) 2016-2019 HERE Europe B.V.

See the [LICENSE](./LICENSE) file in the root of this project for license details.
