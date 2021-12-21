#!/bin/sh

dir=$1
rootdir=$2
onlydir=`basename $dir`

reldir=`echo $dir|sed "s#$rootdir/##"`
out=$reldir/$onlydir-ps.dump
ps axv > $rootdir/$out

echo "##teamcity[publishArtifacts '$out']"
