#!/bin/sh

dir=$1
rootdir=$2

onlydir=`basename $dir`
reldir=`echo $dir|sed "s#$rootdir/##"`

out=$reldir/$onlydir-heap.hprof

parent=$PPID
$JAVA_HOME/bin/jmap -dump:live,format=b,file=$out $parent

echo "##teamcity[publishArtifacts '$out']"
