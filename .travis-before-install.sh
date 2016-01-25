#!/usr/bin/env sh

wget https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-linux-x86_64.tar.bz2
tar jxvf phantomjs-2.1.1-linux-x86_64.tar.bz2

# ensure it works
phantomjs-2.1.1-linux-x86_64/bin/phantomjs  --version
