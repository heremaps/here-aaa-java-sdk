  _  _ ___ ___ ___     _    _    _
 | || | __| _ \ __|   /_\  /_\  /_\
 | __ | _||   / _|   / _ \/ _ \/ _ \
 |_||_|___|_|_\___| /_/ \_\/ \_\/ \_\

HERE Authentication, Authorization, and Accounting

Copyright (c) 2016 HERE Europe B.V.

Introduction
============
This repository contains the complete source code for the here-aaa-sdk project.  Basic 
technical information is contained in this file.

This project is maintained by the HERE Authentication, Authorization, and Accounting team.  For 
questions contact HERE_ACCOUNT_SUPPORT@here.com.

Deliverables
============
The here-aaa-sdk project produces here-oauth-client artifacts distributed via Nexus repository, 
as well as the here-oauth-client-dist-<version>.tar.gz bundle.  External developers currently 
receive the here-oauth-client-dist-<version>.tar.gz bundle as part of onboarding.

Directory Layout
================
Here is an overview of the top-level files contained in the repository:

    |
    +- here-oauth-client      # Source and test code for supported HERE OAuth2.0 flows
       |
       +- src                 # Source and test code
          |
          +- main             # Source code.  The generated JAR file and javadocs are delivered to developers
          |
          +- test             # Test code
    |
    +- examples               # Examples across all projects; these are tutorials intended to be adapted into or inform design of applications
       |
       +- here-oauth-client-example # Tutorial example for here-oauth-client JAR
          |
          +- src              # Source and test code
             |
             +- main          # Source code for the tutorial example
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

Install instructions
--------------------

Open a command prompt at the working tree's root directory and type:

    $ mvn -DskipTests clean install

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

    $ mvn -DargLine='-Dhere.token.endpoint.url=https://stg.account.api.here.com/oauth2/token -Dhere.access.key.id=myclientid -Dhere.access.key.secret=myfailingsecret' clean package

Substitute your Staging here.access.key.id, here.access.key.secret above, to achieve success.

Examples instructions
---------------------
The examples directory contains a tutorial example.  To run it

1. Download and place your HERE Account authorization server credentials.properties file to 
   ~/.here/credentials.properties.
2.   $ chmod 400 ~/.here/credentials.properties
3.   $ java -jar examples/here-oauth-client-example/target/here-oauth-client-example-*[!javadoc].jar

This tutorial uses the recommended "always fresh" approach with the simplest FromFile properties 
loader.  The tutorial will obtain a valid HERE Access Token and print portions of it to stdout.
If in a secure location, optionally re-run with

     $ java -jar examples/here-oauth-client-example/target/here-oauth-client-example-*[!javadoc].jar -v

to print a full valid HERE Access Token to stdout.  You can also put the file in a different 
location or give it a different name, just supply the file as input to the executable jar command 
line.  The examples are for tutorial purposes only and MUST NOT be used in your deployed 
application.  You might find it useful to start from the main(..) method's sample code, and 
adapt the integration to your environment.
 

Developer Usage
===============

Read the javadocs for details.  The mvn commands above will create javadocs locally, which you can 
open via 

    $ open here-oauth-client/target/apidocs/index.html

If you are just getting started, go to com.here.account.oauth2.HereAccount javadocs for the overview 
of two options:
- get an "always fresh" HERE Access Token via TokenEndpoint.requestAutoRefreshingToken(..) approach
- get a one time use HERE Access Token via TokenEndpoint.requestToken(..) approach
