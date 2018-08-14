#!/bin/sh
EXTERN=/home/thomas/projects/extern
JETTY=${EXTERN}/jetty-distribution-9.4.8.v20171121/lib
LOG4J=${EXTERN}/apache-log4j-2.10.0-bin
EPUB=${EXTERN}/epublib
SLF4J=${EXTERN}/slf4j-1.7.25
SERVDIR=/home/thomas/projects/Java/BookDownloadServer

CLASSPATH=${LOG4J}/log4j-api-2.10.0.jar:${LOG4J}/log4j-core-2.10.0.jar:${JETTY}/jetty-http-9.4.8.v20171121.jar:\
${JETTY}/jetty-io-9.4.8.v20171121.jar:${JETTY}/jetty-server-9.4.8.v20171121.jar:${JETTY}/jetty-servlet-9.4.8.v20171121.jar:\
${JETTY}/jetty-util-9.4.8.v20171121.jar:${JETTY}/servlet-api-3.1.jar:${LOG4J}/log4j-slf4j-impl-2.10.0.jar:\
${SLF4J}/slf4j-api-1.7.25.jar:${JETTY}/jetty-security-9.4.8.v20171121.jar:${EPUB}/epublib-core-3.1.jar:\
${SERVDIR}/BookServer.jar:${SERVDIR}/cfg

cd $SERVDIR

exec java -cp $CLASSPATH de.gerner.books.web.server.HttpBookServer
