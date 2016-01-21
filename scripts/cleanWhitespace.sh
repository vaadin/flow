#!/bin/bash

# Must use gsed on Mac
#SED=sed
SED=gsed

rootdir=`dirname $0`/..

for javaFile in `find $rootdir -name "*.java"`
do 
	# Remove whitespace from empty rows
	$SED -i "s/^ [ ]*$//g" $javaFile  

	# Remove trailing whitespace in javadoc
	$SED -i "s/^ \\([ ]*\\)\* $/\\1 \*/g" $javaFile
done
