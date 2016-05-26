#!/bin/bash

# Must use gsed on Mac
#SED=sed
SED=gsed

rootdir=`dirname $0`/..

for file in `find $rootdir -name "*.java"`
do 
	# Add newline at the end if not present
	$SED -i -e '$a\'  $file
done

for file in `find $rootdir -name "*.css"`
do 
	# Add newline at the end if not present
	$SED -i -e '$a\'  $file
done
for file in `find $rootdir -name "*.html"`
do 
	# Add newline at the end if not present
	$SED -i -e '$a\'  $file
done
