#!/bin/bash

if [ "$#" != "1" ]
then
    echo "Usage: $0 <runtime version>"
    exit 1
fi

. `dirname $0`/sed.sh

pushd `dirname $0`/.. > /dev/null
basedir=`pwd`
popd > /dev/null

currentRuntime=`cat "$basedir"/flow-push/pom.xml|grep '<atmosphere.runtime.version>'|cut -d">" -f 2|cut -d"<" -f 1`

echo "Currently using runtime $currentRuntime"

newRuntime=$1

echo "Updating runtime to $newRuntime..."
$SED -i "s#<atmosphere.runtime.version>$currentRuntime<#<atmosphere.runtime.version>$newRuntime<#" "$basedir"/flow-push/pom.xml
$SED -i "s/$currentRuntime/$newRuntime/g" "$basedir"/flow-server/src/main/java/com/vaadin/server/Constants.java
if [[ $newRuntime == *"vaadin"* ]]
then
    $SED -i "s/org.atmosphere/com.vaadin.external.atmosphere/g" "$basedir"/flow-push/pom.xml
else
    $SED -i "s/com.vaadin.external.atmosphere/org.atmosphere/g" "$basedir"/flow-push/pom.xml
fi

