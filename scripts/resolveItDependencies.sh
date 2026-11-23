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
# Without -DskipTests, as that is what activates the it-test-modules profile.

set -u

log=$(mktemp)
mvn -B -ntp -fn dependency:resolve dependency:resolve-plugins \
  -Pit-shared-modules,it-shared-spring-modules -pl flow-tests -amd >"$log" 2>&1
status=$?

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
# could not run at all.
if [ "$status" -ne 0 ] || [ -n "$unexpected" ]; then
  echo "::warning::Resolving the integration test dependencies failed, so the cache may miss some of them"
  printf '%s\n' "$unexpected"
fi
rm -f "$log"

# Markers of failed downloads would make the jobs restoring this cache trust
# the failure instead of trying again.
find ~/.m2/repository -name '*.lastUpdated' -delete
