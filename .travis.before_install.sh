#!/usr/bin/env bash

# Disable all Travis default repositories
sed -i "s/activeByDefault>true</activeByDefault>false</g"  ~/.m2/settings.xml

if [ "$USE_SELENOID" == "true" ]
then
    # Get fresh Docker version
    sudo apt-get update
    sudo apt-get -y -o Dpkg::Options::="--force-confnew" install docker-engine

    # Generate browser config + download docker images for browsers that will be launched on hub lately
    mkdir -p `pwd`/target/selenoid/
    docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aerokube/cm:1.0.0 selenoid \
      --last-versions 1 --tmpfs 256 --pull > `pwd`/target/selenoid/browsers.json

    # Run Selenoid (https://github.com/aerokube/selenoid)
    docker run --name selenoid -d -p 4444:4444 -v `pwd`/target/selenoid:/etc/selenoid:ro \
          -v /var/run/docker.sock:/var/run/docker.sock aerokube/selenoid -limit 10
fi
