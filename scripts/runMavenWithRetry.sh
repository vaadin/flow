#!/usr/bin/env bash
#
# Runs Maven detached from the calling shell, logging to a file that is also
# followed on stdout, and runs it again when it failed because a repository
# could not be reached.
#
# Usage: runMavenWithRetry.sh <log file> <maven arguments...>
#
# Maven is detached so that a JVM it forks and leaves behind (an application
# server, a Quarkus or Spring Boot application) cannot pin the step open by
# holding a pipe to tee, and so that the exit status is Maven's own rather
# than that of a pipe.
#
# The resolver already retries a single download (see MAVEN_ARGS in
# validation.yml), but when Central keeps refusing a runner for longer than
# those retries last, the build stops before any test has run. Only such a
# transfer failure is retried here: a build that failed for any other reason,
# such as a failing test, is reported as it is.

set -u

LOG=$1
shift
MAX_ATTEMPTS=${MAVEN_MAX_ATTEMPTS:-3}

: > "$LOG"
attempt=1
while true; do
  offset=$(stat -c %s "$LOG")
  mvn "$@" </dev/null >>"$LOG" 2>&1 &
  MVN_PID=$!
  tail -c +$((offset + 1)) -f --pid=$MVN_PID "$LOG" &
  TAIL_PID=$!
  wait $MVN_PID
  status=$?
  wait $TAIL_PID
  if [ $status -eq 0 ] || [ $attempt -ge "$MAX_ATTEMPTS" ]; then
    exit $status
  fi
  if ! tail -c +$((offset + 1)) "$LOG" \
      | grep -qE 'Could not transfer (artifact|metadata)'; then
    exit $status
  fi
  delay=$((attempt * 30))
  echo "::warning::Maven could not download from a repository" \
    "(attempt $attempt of $MAX_ATTEMPTS), retrying in ${delay}s"
  sleep $delay
  attempt=$((attempt + 1))
done
