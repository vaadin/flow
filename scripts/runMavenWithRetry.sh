#!/usr/bin/env bash
#
# Runs Maven detached from the calling shell, logging to a file that is also
# followed on stdout, and runs it again when it failed because a repository
# could not be reached.
#
# Usage: runMavenWithRetry.sh <log file> <maven arguments...>
#
# MAVEN_MAX_ATTEMPTS sets the number of attempts (default 3). When
# MAVEN_RETRY_DEADLINE is set to a time in seconds since the epoch, a retry
# is only started when it can end before then: a job that is cancelled by its
# timeout halfway through a retry reports less than the transfer error that
# caused it. A retry is assumed to take MAVEN_RETRY_DURATION seconds, or as
# long as the failed attempt when that took longer. A transfer error usually
# stops the build within its first minute, so the failed attempt alone says
# little about how long a full run takes: set MAVEN_RETRY_DURATION to a
# generous estimate of a full run, plus any later build in the job that
# still has to run before the deadline. Steps that only collect reports
# afterwards belong in the margin between the deadline and the timeout.
#
# Maven is detached so that a JVM it forks and leaves behind (an application
# server, a Quarkus or Spring Boot application) cannot pin the step open by
# holding a pipe to tee, and so that the exit status is Maven's own rather
# than that of a pipe.
#
# The resolver already retries a single download (see MAVEN_ARGS in
# validation.yml), but when Central keeps refusing a runner for longer than
# those retries last, the build stops before any test has run. Only a build
# that such a transfer error stopped is retried here: a build that failed for
# any other reason is reported as it is. With -fae the reactor goes on after
# the module that hit the transfer error, so a build that also has failing
# tests or Javadoc errors is reported as it is too, rather than rerun with
# those failures.

set -u

LOG=$1
shift
MAX_ATTEMPTS=${MAVEN_MAX_ATTEMPTS:-3}

: > "$LOG"
attempt=1
while true; do
  offset=$(stat -c %s "$LOG")
  started=$(date +%s)
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
  # Only an error counts: the same message is also logged as a warning when
  # snapshot metadata cannot be read from one of the declared repositories,
  # or when a download succeeded on one of the resolver's own retries.
  if ! tail -c +$((offset + 1)) "$LOG" \
      | grep -qE '^\[ERROR\].*Could not transfer (artifact|metadata)'; then
    exit $status
  fi
  # The goal failure, not the per-class result: a test class that failed and
  # then passed on a rerun is logged as an error too, but is only a flake.
  # A Javadoc error would fail every retry as well, and each retry would log
  # it again for javadocErrorsToJUnit.py to report once more.
  if tail -c +$((offset + 1)) "$LOG" \
      | grep -qE '^\[ERROR\].*(There are test failures|There was a timeout)|^\[ERROR\] \S+\.java:[0-9]+: error: '; then
    exit $status
  fi
  delay=$((attempt * 30))
  now=$(date +%s)
  duration=$((now - started))
  [ "${MAVEN_RETRY_DURATION:-0}" -gt "$duration" ] && duration=$MAVEN_RETRY_DURATION
  if [ -n "${MAVEN_RETRY_DEADLINE:-}" ] \
      && [ $((now + delay + duration)) -gt "$MAVEN_RETRY_DEADLINE" ]; then
    echo "::warning::Maven could not download from a repository," \
      "not enough time left to retry"
    exit $status
  fi
  # A failed attempt can leave behind the application JVMs it started,
  # holding on to the ports the next attempt needs.
  pkill -TERM java || true
  sleep 2
  pkill -KILL java || true
  echo "::warning::Maven could not download from a repository" \
    "(attempt $attempt of $MAX_ATTEMPTS), retrying in ${delay}s"
  sleep $delay
  attempt=$((attempt + 1))
done
