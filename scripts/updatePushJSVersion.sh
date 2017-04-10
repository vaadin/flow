#!/bin/bash


if [ "$#" != "1" ]
then
    echo "Usage: $0 <js version>"
    exit 1
fi
VERSION=$1
DIR=`dirname $0`
GID=com.vaadin.external.atmosphere.client
AID=javascript

ARTIFACT=$GID:$AID:$VERSION:war
mvn dependency:copy -Dartifact=$ARTIFACT -DoutputDirectory=$DIR/vaadinPush

(
echo "window.vaadinPush = window.vaadinPush|| {};"
echo "(function(define) {"
unzip -p $DIR/vaadinPush/$AID-$VERSION.war javascript/atmosphere.js
echo "if (window.console) {"
echo "	window.console.log(\"Vaadin push loaded\");"
echo "}"
echo "}).call(window.vaadinPush);"
) > $DIR/../flow-push/src/main/resources/META-INF/resources/VAADIN/static/push/vaadinPush.js

# Minify


curl -s http://central.maven.org/maven2/com/yahoo/platform/yui/yuicompressor/2.4.8/yuicompressor-2.4.8.jar > $DIR/vaadinPush/yuicompressor-2.4.8.jar
java -jar $DIR/vaadinPush/yuicompressor-2.4.8.jar  $DIR/../flow-push/src/main/resources/META-INF/resources/VAADIN/static/push/vaadinPush.js  > $DIR/../flow-push/src/main/resources/META-INF/resources/VAADIN/static/push/vaadinPush-min.js


rm -f $DIR/vaadinPush/yuicompressor-2.4.8.jar
rm -f $DIR/vaadinPush/$AID-$VERSION.war
