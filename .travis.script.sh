#!/usr/bin/env bash

# TRAVIS_PULL_REQUEST == "false" for a normal branch commit, the PR number for a PR
# TRAVIS_BRANCH == target of normal commit or target of PR
# TRAVIS_SECURE_ENV_VARS == true if encrypted variables, e.g. SONAR_HOST is available
# TRAVIS_REPO_SLUG == the repository, e.g. vaadin/vaadin

# Collect all commit hashes only for this branch. (no-merges or merged pull requests that have '(#' in the comment)
actualCommits=`git log --no-merges --pretty=oneline master^..HEAD | grep -v "(#" | awk '{print $1}' | tr '\n' ' '`

# Get changed files with full path for branch commits.
change=`diff <(git show --name-only $actualCommits) <(git show --summary $actualCommits)`

# Collect changed modules to build and build also their required modules

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
    if [[ $change == *"flow-client/"* ]]
    then
      modules="$modules -pl flow-client"
    else
      if [[ $change == *"flow-test-util/"* ]]
      then
        modules="$modules -pl flow-test-util"
      else
        if [[ $change == *"flow-tests/"* ]]
        then
          # Building flow as Expense-manager-imperative fails to get its
          # flow dependency on travis!
          # Change this to '-pl flow-tests' when expense manager is removed!
          modules="$modules -pl flow"
        fi
      fi
    fi

    if [[ $change == *"flow-html-components/"* ]]
    then
      modules="$modules -pl flow-html-components"
    else
      if [[ $change == *"flow-documentation/"* ]]
      then
        modules="$modules -pl flow-documentation"
      fi
    fi

    if [[ $change == *"flow-components/"* ]]
    then
      echo "Setting components flag to true"
      modules="$modules -pl flow-components"
    fi
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
    echo "Running Sonar"
    mvn -B -e -V -P validation -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE -Dtest.excludegroup= -Dsonar.verbose=true -Dsonar.analysis.mode=issues -Dsonar.github.repository=$TRAVIS_REPO_SLUG -Dsonar.host.url=$SONAR_HOST -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH -Dsonar.login=$SONAR_LOGIN -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST clean org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar javadoc:javadoc $modules -amd
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]
then
    # master build
    mvn -B -e -V -Dmaven.javadoc.skip=false -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE -Pall-tests -Dgatling.skip=true clean org.jacoco:jacoco-maven-plugin:prepare-agent install $modules -amd
    # Sonar should be run after the project is built so that findbugs can analyze compiled sources
    mvn -B -e -V -Dmaven.javadoc.skip=false -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE -Dgatling.skip=true -Dsonar.verbose=true -Dsonar.analysis.mode=publish -Dsonar.host.url=$SONAR_HOST -Dsonar.login=$SONAR_LOGIN sonar:sonar $modules -amd
else
    # Something else than a "safe" pull request
    mvn -B -e -V -Dmaven.javadoc.skip=false -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE -Pall-tests verify javadoc:javadoc $modules -amd
fi
