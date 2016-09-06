#!/bin/bash

SCRIPTDIR=`dirname $0`

$JAVA_HOME/bin/java -cp ${SCRIPTDIR}/here-oauth-client-example-${my.version}.jar:${SCRIPTDIR}/../here-oauth-client-${my.version}.jar com.here.account.oauth2.tutorial.GetHereClientCredentialsAccessTokenTuturial $@
