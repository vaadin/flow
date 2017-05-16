#!/bin/sh

# TRAVIS_BRANCH == target of normal commit or target of PR
# TRAVIS_REPO_SLUG == the repository, e.g. vaadin/vaadin

while [ "$1" != "" ]; do
    case $1 in
         -javadoc )      shift
                         javadoc=1
                         ;;
         -full )         shift
                         full=1
                         ;;
    esac
    shift
done

export DISPLAY=:99.0
sh -e /etc/init.d/xvfb start

if [ "$javadoc" = "1" ]; then
   javadoc_opts=""
else 
   javadoc_opts=" -Dmaven.javadoc.skip=false javadoc:javadoc"
fi

if [ "$full" = "1" ]; then
mvn -B -e -V -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE  -Pall-tests verify $javadoc_opts
else
mvn -B -e -V -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE -P validation verify $javadoc_opts
fi
