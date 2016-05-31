#!/bin/bash

. `dirname $0`/sed.sh

rootdir=`dirname $0`/..

for file in `find $rootdir -name "*.java" -o -name "*.css" -o -name "*.html"`
do
	# Add newline at the end if not present
	$SED -i -e '$a\'  $file
done
