#!/usr/bin/env bash

# Disable all Travis default repositories
sed -i "s/activeByDefault>true</activeByDefault>false</g"  ~/.m2/settings.xml

# Run Selenoid (https://github.com/aerokube/selenoid)
docker run -d -p 4444:4444 -v `pwd`/selenoid:/etc/selenoid:ro \
      -v /var/run/docker.sock:/var/run/docker.sock aerokube/selenoid:1.3.1
