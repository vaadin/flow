#!/bin/bash

DEPLOYMENT_REPO="hummingbird-snapshot::default::https://repo.vaadin.com/nexus/content/repositories/flow-snapshots/"

if [ "$TRAVIS_PULL_REQUEST" == "false" ]
then
	mvn -B -e -V -DskipTests -DskipITs -Djetty.skip -Dgatling.skip -T 10 -DaltDeploymentRepository=$DEPLOYMENT_REPO deploy --settings settings.xml
fi
