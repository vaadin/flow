#!/bin/bash

DIR=`dirname $0`
GID=com.vaadin.external.atmosphere.client
AID=jquery
VERSION=2.2.6.vaadin4

ARTIFACT=$GID:$AID:$VERSION:war
mvn dependency:copy -Dartifact=$ARTIFACT -DoutputDirectory=$DIR/vaadinPush

(
echo "(function(define) {"
cat $DIR/vaadinPush/jquery-1.11.0.js
echo "window.jQueryVaadin = window.jQuery.noConflict(true);"
echo "(function(jQuery, undefined) {"
unzip -p $DIR/vaadinPush/$AID-$VERSION.war jquery/jquery.atmosphere.js
echo "})(jQueryVaadin);"
echo "if (window.console) {"
echo "	window.console.log(\"Vaadin push loaded\");"
echo "}"
echo "})();"
) > $DIR/../hummingbird-push/src/main/resources/VAADIN/push/vaadinPush.js

# Minify


curl -s http://central.maven.org/maven2/com/yahoo/platform/yui/yuicompressor/2.4.8/yuicompressor-2.4.8.jar > $DIR/vaadinPush/yuicompressor-2.4.8.jar
java -jar $DIR/vaadinPush/yuicompressor-2.4.8.jar  $DIR/../hummingbird-push/src/main/resources/VAADIN/push/vaadinPush.js  > $DIR/../hummingbird-push/src/main/resources/VAADIN/push/vaadinPush-min.js 


rm -f $DIR/vaadinPush/yuicompressor-2.4.8.jar
rm -f $DIR/vaadinPush/$AID-$VERSION.war