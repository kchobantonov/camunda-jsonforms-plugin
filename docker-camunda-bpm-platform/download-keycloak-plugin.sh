#!/bin/sh -ex

PROXY=""
if [ -n "$MAVEN_PROXY_HOST" ] ; then
	PROXY="-DproxySet=true"
	PROXY="$PROXY -Dhttp.proxyHost=$MAVEN_PROXY_HOST"
	PROXY="$PROXY -Dhttps.proxyHost=$MAVEN_PROXY_HOST"
	if [ -z "$MAVEN_PROXY_PORT" ] ; then
		echo "ERROR: MAVEN_PROXY_PORT must be set when MAVEN_PROXY_HOST is set"
		exit 1
	fi
	PROXY="$PROXY -Dhttp.proxyPort=$MAVEN_PROXY_PORT"
	PROXY="$PROXY -Dhttps.proxyPort=$MAVEN_PROXY_PORT"
	echo "PROXY set Maven proxyHost and proxyPort"
	if [ -n "$MAVEN_PROXY_USER" ] ; then
		PROXY="$PROXY -Dhttp.proxyUser=$MAVEN_PROXY_USER"
		PROXY="$PROXY -Dhttps.proxyUser=$MAVEN_PROXY_USER"
		echo "PROXY set Maven proxyUser"
	fi
	if [ -n  "$MAVEN_PROXY_PASSWORD" ] ; then
		PROXY="$PROXY -Dhttp.proxyPassword=$MAVEN_PROXY_PASSWORD"
		PROXY="$PROXY -Dhttps.proxyPassword=$MAVEN_PROXY_PASSWORD"
		echo "PROXY set Maven proxyPassword"
	fi
fi

case ${DISTRO} in
    tomcat*) 
        # download camunda keycloak extension. 
        mvn dependency:copy -B \
            $PROXY \
            -Dartifact="org.camunda.bpm.extension:camunda-platform-7-keycloak-all:${CAMUNDA_KEYCLOAK_VERSION}:jar" \
            -DoutputDirectory=/tmp/

        cp /tmp/camunda-platform-7-keycloak-all-${CAMUNDA_KEYCLOAK_VERSION}.jar /camunda/lib/camunda-platform-7-keycloak-all.jar
        cp /tmp/bpm-platform.xml /camunda/conf/bpm-platform.xml
    ;;
esac

