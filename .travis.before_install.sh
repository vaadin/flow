#!/usr/bin/env bash

# Disable all Travis default repositories
sed -i "s/activeByDefault>true</activeByDefault>false</g"  ~/.m2/settings.xml
export CHROME_BIN=/usr/bin/google-chrome
export DISPLAY=:99.0
sh -e /etc/init.d/xvfb start
# The Following line fixes random Chrome startup failures (https://github.com/SeleniumHQ/docker-selenium/issues/87)
export DBUS_SESSION_BUS_ADDRESS=/dev/null
