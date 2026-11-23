#!/bin/bash
#
# Resolves the dependencies and plugins the flow-tests modules declare into the
# local Maven repository, so that a CI cache saved afterwards holds them. Used
# by validation.yml and warm-caches.yml; see the notes on those steps.
#
# Expects Flow itself to be installed already. The flow-tests modules are not
# packaged, so the ones that depend on another of them fail to resolve it:
# --fail-never keeps going past those, including into the modules that depend
# on them, which --fail-at-end would skip. Any other resolution error, such as
# Maven Central answering with HTTP 429, is reported as a workflow warning
# rather than failing the job, since the jobs that need the artifacts resolve
# them again anyway.
#
# The plugins are resolved in a build of their own: a module whose
# dependencies fail to resolve skips its remaining goals, so in the same build
# the plugins of every module depending on another flow-tests module, such as
# the dev loop fixture apps and their server plugins, would never be resolved.
# Plugins do not need the reactor's snapshots, so on their own they all
# resolve.
#
# Without -DskipTests, as that is what activates the it-test-modules profile.
#
# The Quarkus suite is resolved in a build of its own, from its own root, as
# it is not a module of the flow-tests reactor; see flow-tests/pom.xml. Most of
# its modules depend on fixture modules of the suite, and dependency:resolve
# alone would fail each of them before resolving anything, so the suite is
# compiled first for the reactor to supply them. The go-offline goal adds
# what the Quarkus build resolves by itself, above all the deployment
# artifacts of the extensions, which dependency:resolve never sees. The
# platform descriptor is fetched on its own, as only the codestarts tests
# read it, while they run. The Flow frontend goals are skipped since they
# resolve nothing, and the build output is removed afterwards so that the
# workspace the build job saves does not carry it.

set -u

log=$(mktemp)
mvn -B -ntp -fn dependency:resolve \
  -Pit-shared-modules,it-shared-spring-modules -pl flow-tests -amd >"$log" 2>&1
status=$?
mvn -B -ntp -fn dependency:resolve-plugins \
  -Pit-shared-modules,it-shared-spring-modules -pl flow-tests -amd >>"$log" 2>&1
plugins_status=$?

quarkus_pom=flow-tests/vaadin-quarkus-tests/pom.xml
quarkus_version=$(sed -n 's:.*<quarkus.version>\(.*\)</quarkus.version>.*:\1:p' pom.xml)
mvn -B -ntp -fn -f "$quarkus_pom" -Dvaadin.skip test-compile \
  dependency:resolve dependency:resolve-plugins \
  "io.quarkus:quarkus-maven-plugin:$quarkus_version:go-offline" >>"$log" 2>&1
quarkus_status=$?
mvn -B -ntp -N -f "$quarkus_pom" dependency:get -Dtransitive=false \
  "-Dartifact=io.quarkus:quarkus-bom-quarkus-platform-descriptor:$quarkus_version:json:$quarkus_version" \
  >>"$log" 2>&1
descriptor_status=$?
[ "$status" -eq 0 ] && status=$plugins_status
[ "$status" -eq 0 ] && status=$quarkus_status
[ "$status" -eq 0 ] && status=$descriptor_status
mvn -B -ntp -q -f "$quarkus_pom" clean

# The snapshots of this reactor are the only artifacts expected to be missing.
# A module that fails can miss several artifacts at once, so every one of them
# is listed before filtering: each `dependency:` line Maven prints, the entries
# of a `following artifacts could not be resolved:` list, and any artifact
# named alone. Filtering whole messages instead would let a missing snapshot
# hide an external artifact that failed beside it.
unexpected=$(
  {
    grep -oE '^\[ERROR\] dependency: [^ ]+' "$log" | awk '{ print $3 }'
    grep 'following artifacts could not be resolved' "$log" \
      | grep -oE '[^ ,:]+(:[^ ,:]+){3,4} \((absent|present[^)]*)\)' | awk '{ print $1 }'
    grep -oE 'Could not (transfer|find) artifact [^ ]+' "$log" | awk '{ print $5 }'
    grep -oE 'Plugin [^ ]+ or one of its dependencies could not be resolved' "$log" \
      | awk '{ print "plugin " $2 }'
  } | grep -vE '^com\.vaadin:[^ ]*-SNAPSHOT$' | sort -u
)
# --fail-never exits with 0 whatever failed, so a non-zero status means Maven
# could not run at all, or that the platform descriptor could not be fetched.
if [ "$status" -ne 0 ] || [ -n "$unexpected" ]; then
  echo "::warning::Resolving the integration test dependencies failed, so the cache may miss some of them"
  printf '%s\n' "$unexpected"
fi
rm -f "$log"

# Markers of failed downloads would make the jobs restoring this cache trust
# the failure instead of trying again.
find ~/.m2/repository -name '*.lastUpdated' -delete
