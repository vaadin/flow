#!/bin/bash
set -eo pipefail

case $1 in
  install-tools)
    echo ">>>>>>>>>>> Installing Tools ..."
    
    test -n "$TB_LICENSE"
    mkdir -p ~/.vaadin/
    echo '{"username":"'`echo $TB_LICENSE | cut -d / -f1`'","proKey":"'`echo $TB_LICENSE | cut -d / -f2`'"}' > ~/.vaadin/proKey

    wget -q https://deb.nodesource.com/setup_14.x -O - | sudo bash
    sudo apt-get install -y nodejs

    wget -q https://archive.apache.org/dist/maven/maven-3/3.8.3/binaries/apache-maven-3.8.3-bin.tar.gz -O - | tar -C ~ -xzf -
    sudo rm -f /opt/apache-maven
    sudo ln -s ~/apache-maven-3.8.3 /opt/apache-maven
    ;;

  get-flow-modules)
    H=`mvn help:evaluate -Dexpression=project.modules -DskipTests -Prun-tests \
        | grep "<\/string>" \
        | grep -v flow-tests \
        | grep -v flow-client \
        | grep -v flow-jandex \
        | sed -e 's, *<string>\(.*\)</string>,\1,g' \
        | sort`
    echo ">>>>>>>>>>> Flow Modules:" >&2
    echo "$H" >&2
    echo "$H"
    ;;

  get-it-modules)
    H=`mvn help:evaluate -Dexpression=project.modules -pl flow-tests -DskipTests -Prun-tests \
        | grep "<\/string>" \
        | grep -v test-mixed/pom-npm.xml \
        | grep -v test-jetty-reload \
        | grep -v test-root-ui-context \
        | grep -v test-root-context \
        | sed -e 's, *<string>\(.*\)</string>,flow-tests/\1,g' \
        | sort`
    echo ">>>>>>>>>>> IT Modules:" >&2
    echo "$H" >&2
    echo "$H"
    ;;

  *)
    echo "Usage $0 <install-tools | get-flow-modules | get-it-modules>" >&2
    exit 1
    ;;
esac

