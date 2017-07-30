#!/usr/bin/env bash

# TRAVIS_PULL_REQUEST == "false" for a normal branch commit, the PR number for a PR
# TRAVIS_BRANCH == target of normal commit or target of PR
# TRAVIS_SECURE_ENV_VARS == true if encrypted variables, e.g. SONAR_HOST is available
# TRAVIS_REPO_SLUG == the repository, e.g. vaadin/vaadin

# Get all changes to branch (no-merges)
actualCommits=`git log --no-merges --pretty=oneline master^..HEAD`

# If running a pull request drop merged pull requests that have '(#' in the comment)
if [[ "$TRAVIS_PULL_REQUEST" != "false" ]]
then
  actualCommits=`echo "$actualCommits" | grep -v "(#"`
fi

#Collect commit hashes
actualCommits=`echo "$actualCommits" | awk '{print $1}' | tr '\n' ' '`

# Get changed files with full path for branch commits.
change=`diff <(git show --name-only $actualCommits) <(git show --summary $actualCommits)`

# Collect changed modules to build and build also modules that depend on the
# selected modules (see the flag '-amd')
modules=
if [[ $change == *"flow-push/"* ]]
then
  modules="$modules -pl flow-push"
else
  ## All modules except for production mode depend on push.
  if [[ $change == *"flow-server/"* ]]
  then
    modules="$modules -pl flow-server"
  else
    if [[ $change == *"flow-html-components/"* ]]
    then
      modules="$modules -pl flow-html-components"
    else
      if [[ $change == *"flow-documentation/"* ]]
      then
        modules="$modules -pl flow-documentation"
      fi
    fi

    if [[ $change == *"flow-components-parent/"* ]]
    then
      # flow-components-parent imports modules from flow-components, so we need to build it also
      modules="$modules -pl flow-components"
      ## only trigger the analyzer & generator for validation builds on PRs that touched component generation
      echo "Setting components flag to true"
      modules="$modules -pl flow-components-parent -P generator"
    fi

    if [[ $change == *"flow-client/"* ]]
    then
      modules="$modules -pl flow-client"
    fi
  fi
fi

if [[ $change == *"flow-test-util/"* ]]
then
  modules="$modules -pl flow-test-util"
else
  if [[ $change == *"flow-tests/"* ]]
  then
    modules="$modules -pl flow-tests"
  fi
fi

if [[ $change == *"flow-server-production-mode/"* ]]
then
  modules="$modules -pl flow-server-production-mode"
fi

if [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ "$TRAVIS_SECURE_ENV_VARS" == "true" ] 
then
    # Pull request for master with secure vars (SONAR_GITHUB_OAUTH, SONAR_HOST) available

    # Trigger Sonar analysis
    # Verify build and build javadoc
    echo "Running clean verify $modules -amd"
    mvn -B -e -V \
        -Pvalidation \
        -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE \
        -Dtest.excludegroup= \
        -Dtest.use.hub=true \
        -Dcom.vaadin.testbench.Parameters.hubHostname="localhost" \
        clean \
        org.jacoco:jacoco-maven-plugin:prepare-agent verify javadoc:javadoc $modules -amd
    # Get the status for the previous maven command and if not exception then run sonar.
    STATUS=$?
    if [ $STATUS -eq 0 ]
    then
        if [ "$SKIP_SONAR" != "true" ]
        then
            # Run sonar
            echo "Running Sonar"
            mvn -B -e -V \
                -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
                -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
                -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
                -Dgatling.skip=true \
                -Dsonar.verbose=true \
                -Dsonar.analysis.mode=issues \
                -Dsonar.host.url=$SONAR_HOST \
                -Dsonar.login=$SONAR_LOGIN \
                -DskipTests \
                compile sonar:sonar
        else
            echo "Skipping sonar."
        fi
    else
        echo "Build failed, skipping sonar."
        exit 1
    fi
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]
then
    # master build
    mvn -B -e -V \
        -Pall-tests \
        -Dmaven.javadoc.skip=false \
        -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE \
        -Dgatling.skip=true \
        -Dtest.use.hub=true \
        -Dcom.vaadin.testbench.Parameters.hubHostname="localhost" \
        clean org.jacoco:jacoco-maven-plugin:prepare-agent install

    # Get the status for the previous maven command and if not exception then run sonar.
    STATUS=$?
    if [ $STATUS -eq 0 ]
    then
        if [ "$SKIP_SONAR" != "true" ]
        then    
            # Sonar should be run after the project is built so that findbugs can analyze compiled sources
            echo "Running Sonar"
            mvn -B -e -V \
                -Dmaven.javadoc.skip=false \
                -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE \
                -Dgatling.skip=true \
                -Dsonar.verbose=true \
                -Dsonar.analysis.mode=publish \
                -Dsonar.host.url=$SONAR_HOST \
                -Dsonar.login=$SONAR_LOGIN \
                -DskipTests \
                compile sonar:sonar
        else
            echo "Skipping sonar."
        fi
    else
        echo "Build failed, skipping sonar."
        exit 1
    fi
else
    # Something else than a "safe" pull request
    mvn -B -e -V \
        -Pall-tests \
        -Dmaven.javadoc.skip=false \
        -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE \
        -Dtest.use.hub=true \
        -Dcom.vaadin.testbench.Parameters.hubHostname="localhost" \
        verify javadoc:javadoc $modules -amd
fi
