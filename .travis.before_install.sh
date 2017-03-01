#!/usr/bin/env bash

# Disable all Travis default repositories 
sed -i "s/activeByDefault>true</activeByDefault>false</g"  ~/.m2/settings.xml
export CHROME_BIN=/usr/bin/google-chrome
sh -e /etc/init.d/xvfb start
