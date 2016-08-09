  _  _ ___ ___ ___     _    _    _
 | || | __| _ \ __|   /_\  /_\  /_\
 | __ | _||   / _|   / _ \/ _ \/ _ \
 |_||_|___|_|_\___| /_/ \_\/ \_\/ \_\

HERE Authentication, Authorization, and Accounting

Introduction
============
here-oauth-client JAR
- Authentication features for OAuth1 signature to the HERE OAuth2 Authorization Server.
- Authorization features for OAuth2.0 client_credentials grant_type for confidential clients to obtain 
  HERE Access Tokens from the HERE OAuth2.0 Authorization Server, including the ability to automatically 
  refresh HERE Access Tokens.
- Authorization features for using OAuth2.0 Bearer tokens in the Authorization header to HERE Services
- Accounting within HERE Services using digitally signed claims from the HERE Access Token

For help, contact HERE_ACCOUNT_SUPPORT@here.com.
Built using Apache Maven (https://maven.apache.org/)

Developer Setup
===============

1. Requires Java 1.8.

Configuration
=============

The tests use command-line arguments for configuration.  Please run

  mvn -DargLine='-Dhere.token.endpoint.url=https://stg.account.api.here.com/oauth2/token -Dhere.access.key.id=myclientid -Dhere.access.key.secret=myfailingsecret' clean package

to demonstrate a failing credential.  This test is intended to fail.  
Substitute your Staging here.access.key.id, here.access.key.secret above, to achieve success.

Developer Usage
===============

Read the javadocs for details.  The mvn command above will create javadocs locally, which you can open via 

  open here-oauth-client/target/apidocs/index.html

If you are just getting started, go to com.here.account.oauth2.HereAccount.java javadocs for the overview of two options:
- get an "always fresh" HERE Access Token via TokenEndpoint.requestAutoRefreshingToken(..) approach
- get a one time use HERE Access Token via TokenEndpoint.requestToken(..) approach

Sub-Projects
============

1. Please run

  ls

