#!/usr/bin/env bash

# USE_SELENOID == use Docker and Selenoid to run IT tests in personal hub
# SELENOID_VERSION == specify a particular Docker Selenoid image version

# Disable all Travis default repositories
sed -i "s/activeByDefault>true</activeByDefault>false</g"  ~/.m2/settings.xml

if [ "$USE_SELENOID" == "true" ]
then
    # Get fresh Docker and jq
    sudo apt-get update
    sudo apt-get -y -o Dpkg::Options::="--force-confnew" install docker-engine jq

    # Parse browser config using jq and download all docker images
    cat ./browsers.json | jq -r '..|.image?|strings' | xargs -I{} docker pull {}

    # Determine Docker Selenoid image version, full version list: https://hub.docker.com/r/aerokube/selenoid/tags/
    selenoidVersion=${SELENOID_VERSION}
    if [ -z ${selenoidVersion} ]; then
        selenoidVersion="latest-release"
    fi

    # Run Selenoid (https://github.com/aerokube/selenoid)
    docker run -d -p 4444:4444 -v `pwd`:/etc/selenoid:ro \
          -v /var/run/docker.sock:/var/run/docker.sock aerokube/selenoid:${selenoidVersion}
fi
