  _  _ ___ ___ ___     _    _    _
 | || | __| _ \ __|   /_\  /_\  /_\
 | __ | _||   / _|   / _ \/ _ \/ _ \
 |_||_|___|_|_\___| /_/ \_\/ \_\/ \_\

HERE Authentication, Authorization, and Accounting

Authentication https://confluence.in.here.com/display/HEREAccount
Authorization  https://confluence.in.here.com/display/APIPlatform/Sentry
Accounting     https://confluence.in.here.com/display/APIPlatform/Unity

For help, contact HERE_ACCOUNT_SUPPORT@here.com and ampsupport@here.com.
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

