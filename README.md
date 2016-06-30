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

  mvn -DargLine='-DurlStart=https://stg.account.api.here.com -DclientId=myclientid -DclientSecret=mysupersecret -Demail=mytest1234@example.com -Dpassword=mypassword' clean test

to demonstrate a failing credential.  This test is intended to fail.  
Substitute your Staging clientId, clientSecret, email, and password above, to achieve success.

Sub-Projects
============

1. Please run

  ls

