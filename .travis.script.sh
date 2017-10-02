#!/usr/bin/env bash

# TRAVIS_PULL_REQUEST == "false" for a normal branch commit, the PR number for a PR
# TRAVIS_BRANCH == target of normal commit or target of PR
# TRAVIS_SECURE_ENV_VARS == true if encrypted variables, e.g. SONAR_HOST is available
# TRAVIS_REPO_SLUG == the repository, e.g. vaadin/vaadin
# SKIP_SONAR == skip sonar checks
# USE_SELENOID == use Docker and Selenoid to run IT tests in personal hub

# Exclude third party code from Sonar analysis
SONAR_EXCLUSIONS=**/bower_components/**,**/node_modules/**,**/node/**,**/src/main/webapp/**

function getDockerParamsIfNeeded {
    if [ "$USE_SELENOID" == "true" ]
    then
        echo "-Dtest.use.hub=true \
            -Dcom.vaadin.testbench.Parameters.hubHostname=localhost"
    else
        echo ""
    fi
}

function getSonarDetails {
    if [ "$TRAVIS_PULL_REQUEST" != "false" ]
    then
        echo "-Dsonar.github.repository=$TRAVIS_REPO_SLUG \
               -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
               -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
               -Dsonar.analysis.mode=issues"
    else
        echo "-Dsonar.analysis.mode=publish"
    fi
}

function runSonar {
    if [ "$SKIP_SONAR" != "true" ]
    then
        # Sonar should be run after the project is built so that findbugs can analyze compiled sources
        echo "Running Sonar"
        mvn -B -e -V \
            -Dmaven.javadoc.skip=false \
            -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE \
            -Dsonar.verbose=true \
            -Dsonar.host.url=$SONAR_HOST \
            -Dsonar.login=$SONAR_LOGIN \
            -Dsonar.exclusions=$SONAR_EXCLUSIONS \
            $(getSonarDetails) \
            -DskipTests \
            compile sonar:sonar
    else
        echo "SKIP_SONAR env variable is set to 'true', skipping sonar."
    fi
}

if [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]
then
    # Pull request for master with secure vars (SONAR_GITHUB_OAUTH, SONAR_HOST) available

    # Trigger Sonar analysis
    # Verify build and build javadoc
    echo "Running clean verify"
    mvn -B -e -V \
        -Pvalidation \
        -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE \
        -Dtest.excludegroup= \
        $(getDockerParamsIfNeeded) \
        clean \
        license:download-licenses \
        org.jacoco:jacoco-maven-plugin:prepare-agent verify javadoc:javadoc
    # Get the status for the previous maven command and if not exception then run sonar.
    STATUS=$?
    if [ $STATUS -eq 0 ]
    then
        runSonar
    else
        echo "Build failed, skipping sonar."
        exit 1
    fi
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]
then
    # master build
    # Production mode commented out due to https://github.com/vaadin/flow/issues/2165
    # -Dvaadin.productionMode=true \
    mvn -B -e -V \
        -Pall-tests \
        -Dmaven.javadoc.skip=false \
        -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE \
        $(getDockerParamsIfNeeded) \
        clean org.jacoco:jacoco-maven-plugin:prepare-agent license:download-licenses install

    # Get the status for the previous maven command and if not exception then run sonar.
    STATUS=$?
    if [ $STATUS -eq 0 ]
    then
       runSonar
    else
        echo "Build failed, skipping sonar."
        exit 1
    fi
else
    # Something else than a "safe" pull request
    mvn -B -e -V \
        -Pall-tests \
        -Dmaven.javadoc.skip=false \
        $(getDockerParamsIfNeeded) \
        -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE \
        license:download-licenses \
        verify javadoc:javadoc
fi
