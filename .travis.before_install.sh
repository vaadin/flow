#!/usr/bin/env bash

# USE_SELENOID == use Docker and Selenoid to run IT tests in personal hub
# SELENOID_VERSION == specify a particular Docker Selenoid image version
# CHROME_DEFAULT_URL == specify a default url that will be used to download Chrome when local distribution cannot be found

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
else
    custom_chrome_dir=/tmp/custom_chrome
    chrome_package_name=google-chrome-stable_current_amd64.deb
    mkdir -p ${custom_chrome_dir}
    scp -o "StrictHostKeyChecking no" -P 5177 dev@virtuallypreinstalled.com:/home/dev/custom_chrome/${chrome_package_name} ${custom_chrome_dir}

    if [ ! -f ${custom_chrome_dir}/${chrome_package_name} ]; then
        echo "Was not able to download custom Chrome binary from the remote server, using the official distribution"
        chrome_default_url=${CHROME_DEFAULT_URL}
        if [ -z ${selenoidVersion} ]; then
            chrome_default_url="http://dl.google.com/dl/linux/direct/google-chrome-stable_current_amd64.deb"
        fi
        wget -O ${custom_chrome_dir} ${chrome_default_url}
    fi

    sudo dpkg -i --force-all ${custom_chrome_dir}/${chrome_package_name}
    rm -rfv ${custom_chrome_dir}
fi
