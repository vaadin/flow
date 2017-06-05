#!/usr/bin/env bash

# TRAVIS_PULL_REQUEST == "false" for a normal branch commit, the PR number for a PR
# TRAVIS_BRANCH == target of normal commit or target of PR
# TRAVIS_SECURE_ENV_VARS == true if encrypted variables, e.g. SONAR_HOST is available
# TRAVIS_REPO_SLUG == the repository, e.g. vaadin/vaadin

# Count all commits only for this branch.
actualBranchCommitAmount=`git log --no-merges --first-parent --pretty=oneline master^..HEAD | wc -l`
# Remove first commit as it is the branch parent which is from master
actualBranchCommitAmount=`expr $actualBranchCommitAmount - 1`

# Get changed files with full path for branch commits.
change=`diff <(git log --no-merges --first-parent --name-only master^..HEAD -$actualBranchCommitAmount) <(git log --no-merges --first-parent --summary master^..HEAD -$actualBranchCommitAmount)`

# Check from the latest commit if this contains changes to components package.
components=false
if [[ $change == *"flow-components/"* ]]
then
  echo "Setting components flag to true"
  components=true
fi

if [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]
then
    # Pull request for master with secure vars (SONAR_GITHUB_OAUTH, SONAR_HOST) available

    # Trigger Sonar analysis
    echo "Running Sonar"
    mvn -B -e -V -P validation -Dtest.components=$components -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE -Dtest.excludegroup= -Dsonar.verbose=true -Dsonar.analysis.mode=issues -Dsonar.github.repository=$TRAVIS_REPO_SLUG -Dsonar.host.url=$SONAR_HOST -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH -Dsonar.login=$SONAR_LOGIN -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST clean org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar javadoc:javadoc
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]
then
    # master build
    mvn -B -e -V -Dtest.components=$components -Dmaven.javadoc.skip=false -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE -Pall-tests -Dgatling.skip=true clean org.jacoco:jacoco-maven-plugin:prepare-agent install
    # Sonar should be run after the project is built so that findbugs can analyze compiled sources
    mvn -B -e -V -Dtest.components=$components -Dmaven.javadoc.skip=false -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE -Dgatling.skip=true -Dsonar.verbose=true -Dsonar.analysis.mode=publish -Dsonar.host.url=$SONAR_HOST -Dsonar.login=$SONAR_LOGIN sonar:sonar
else
    # Something else than a "safe" pull request
    mvn -B -e -V -Dtest.components=$components -Dmaven.javadoc.skip=false -Dvaadin.testbench.developer.license=$TESTBENCH_LICENSE -Pall-tests verify javadoc:javadoc
fi
