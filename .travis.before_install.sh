#!/usr/bin/env bash

# Travis does not have PhantomJS 2.1 installed
if [ ! -e "phantomjs-2.1.1-linux-x86_64/bin/phantomjs" ]
then
	wget https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-linux-x86_64.tar.bz2
	tar jxvf phantomjs-2.1.1-linux-x86_64.tar.bz2
else
	echo "PhantomJS already downloaded"
fi
