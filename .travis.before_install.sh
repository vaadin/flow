#!/usr/bin/env bash

# Disable all Travis default repositories
sed -i "s/activeByDefault>true</activeByDefault>false</g"  ~/.m2/settings.xml

if [ "$USE_SELENOID" == "true" ]
then
    # Get fresh Docker and jq
    sudo apt-get update
    sudo apt-get -y -o Dpkg::Options::="--force-confnew" install docker-engine jq

    # Parse browser config using jq and download all docker images
    cat ./browsers.json | jq -r '..|.image?|strings' | xargs -I{} docker pull {}

    # Run Selenoid (https://github.com/aerokube/selenoid)
    docker run -d -p 4444:4444 -v `pwd`:/etc/selenoid:ro \
          -v /var/run/docker.sock:/var/run/docker.sock aerokube/selenoid:1.3.6 -limit 10
fi
