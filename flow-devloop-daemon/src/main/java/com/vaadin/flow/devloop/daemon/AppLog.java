/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.devloop.daemon;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads the app's own log, so the daemon can report why a launch failed instead
 * of leaving the developer to go and find out.
 * <p>
 * The app's stdout and stderr go straight to {@code target/devloop/app.log},
 * which makes that file the only place the real reason lives: "Port 8080 was
 * already in use" is printed by the app and is not observable from the daemon's
 * side of the process boundary. A failure that says only "exit code 1" and
 * leaves the cause in a file nobody was told to read is exactly what made a
 * port clash look like a regression in the tool.
 */
final class AppLog {

    /**
     * Enough tail to carry the head of a stack trace without flooding a reply.
     */
    private static final int TAIL_LINES = 20;

    /** The end of the log is where failures are; reading it all is wasteful. */
    private static final int TAIL_BYTES = 64 * 1024;

    /**
     * The web server is listening. This is the line a port clash never reaches,
     * and it is printed after the bind, which is what makes it usable as the
     * "the port really is ours" signal.
     */
    private static final Pattern SERVING = Pattern
            .compile("(?i)(tomcat|jetty|netty|undertow).{0,40}started on port"
                    + "|(?i)started \\S+ in \\d+([.,]\\d+)? second");

    /** Spring Boot's failure analyzer puts the plain-words cause under this. */
    private static final Pattern DESCRIPTION = Pattern
            .compile("^Description:\\s*$");

    private static final Pattern THROWN = Pattern
            .compile("\\S*(Exception|Error)(:|\\s|$)");

    /**
     * Where a failure report starts. Everything after it is the trace it
     * explains, which is why the reason has to be looked for from here forwards
     * and not at the end of the log: a Spring context failure prints its reason
     * first and a hundred frames after it.
     */
    private static final Pattern REPORT = Pattern
            .compile("APPLICATION FAILED TO START|Application run failed"
                    + "|Exception encountered during context initialization"
                    + "|\\b(ERROR|SEVERE|FATAL)\\b");

    /**
     * One logged error. The level field of a log line, plus the header a stack
     * trace gets when it reaches stderr with no logger at all. Frames are not
     * matched: they carry no message, and the line above them does.
     */
    private static final Pattern ERROR_LINE = Pattern
            .compile("\\b(ERROR|SEVERE|FATAL)\\b|^Exception in thread ");

    /**
     * A failure from the dev server, which the level field does not reveal.
     * <p>
     * Flow pipes every line Vite writes through {@code DevServerOutputTracker}
     * at {@code INFO}, so a TypeScript syntax error arrives looking exactly
     * like a progress message: the level is {@code INFO}, and the word "error"
     * is lower case where {@link #ERROR_LINE} wants {@code ERROR}. Even the
     * detail line underneath does not match - {@code [PARSE_ERROR]} has no word
     * boundary before {@code ERROR}, because an underscore is a word character.
     * So without this the browser shows a red overlay, the log holds the whole
     * diagnostic, and {@code apply} reports a clean {@code Stable}.
     * <p>
     * Matched on the opening line of a failure rather than on anything
     * containing "error": the report is many lines long - a source excerpt, a
     * caret diagram, a JavaScript stack - and counting each of them would turn
     * one broken file into a dozen errors.
     */
    private static final Pattern DEV_SERVER_ERROR = Pattern
            .compile("\\[vite\\] Internal server error"
                    + "|\\[vite\\] error while updating dependencies"
                    + "|Pre-transform error:"
                    + "|Transform failed with \\d+ error"
                    + "|Build failed with \\d+ error" + "|^error during build:"
                    + "|Failed to resolve import ");

    /**
     * The line that opens a stack trace - the exception's own type and message.
     * A logger prints its message first and the throwable on the line below, so
     * this is where the type is, and the type is what says whether a redefine
     * held. Anchored, so a frame mentioning a class never passes for one.
     */
    private static final Pattern THROWN_HEADER = Pattern
            .compile("^(Caused by: )?[\\w.$]*(Exception|Error)(:|$)");

    /**
     * Errors that mean a redefine did not hold. Deliberately narrow: an app is
     * free to log an error of its own during a request, and turning that into a
     * failed apply would make the verdict worse, not better. These are the ones
     * that only happen when running code and loaded classes disagree - a call
     * landing on a proxy or a caller compiled against a member that is no
     * longer there - plus the bean failures a re-created context throws.
     * Anything else is reported and left to the developer to judge.
     */
    private static final Pattern RELOAD_FAILURE = Pattern
            .compile("AbstractMethodError|NoSuchMethodError|NoSuchFieldError"
                    + "|IncompatibleClassChangeError|LinkageError"
                    + "|NoClassDefFoundError|BeanCreationException"
                    + "|BeanInstantiationException|UnsatisfiedDependencyException");

    /** Errors kept per window; a reply quoting more than this helps nobody. */
    private static final int MAX_ERRORS = 5;

    private AppLog() {
    }

    /** Whether one log line says the app's web server is up. */
    static boolean serving(String line) {
        return SERVING.matcher(line).find();
    }

    /**
     * Whether one log line is a failure the dev server reported.
     * <p>
     * Separable from the app's own errors because it is attributable in a way
     * they are not: Vite compiles on save rather than on apply, so its errors
     * are already in the log by the time an apply looks, and only these may be
     * carried across the window boundary.
     *
     * @param line
     *            a log line
     * @return {@code true} if the dev server reported a failure on it
     */
    static boolean devServerError(String line) {
        return DEV_SERVER_ERROR.matcher(line).find();
    }

    /**
     * Follows a log as it is written. Only the bytes that appeared since the
     * last call are ever read, so a start can poll this every 100 ms for as
     * long as an app takes to boot without the cost growing with the log.
     */
    static final class Cursor {

        private final Path log;
        private long at;
        private String partial = "";

        Cursor(Path log) {
            this.log = log;
        }

        /**
         * The complete lines written since the previous call. A trailing
         * fragment is held back rather than returned as a line, because the app
         * may well be mid-write in the middle of the very line being waited
         * for.
         */
        List<String> drain() {
            String text;
            try (SeekableByteChannel channel = Files.newByteChannel(log,
                    StandardOpenOption.READ)) {
                long available = channel.size() - at;
                if (available <= 0) {
                    return List.of();
                }
                ByteBuffer buffer = ByteBuffer
                        .allocate((int) Math.min(available, TAIL_BYTES));
                channel.position(at);
                int read = channel.read(buffer);
                if (read <= 0) {
                    return List.of();
                }
                at += read;
                text = decode(buffer.array(), read);
            } catch (IOException e) {
                // A log that cannot be read is not a reason to fail a start;
                // the
                // process signals are what decide the outcome.
                return List.of();
            }

            List<String> lines = new ArrayList<>();
            String pending = partial + text;
            int from = 0;
            for (int i = 0; i < pending.length(); i++) {
                if (pending.charAt(i) == '\n') {
                    lines.add(pending.substring(from, i).stripTrailing());
                    from = i + 1;
                }
            }
            partial = pending.substring(from);
            return lines;
        }
    }

    /**
     * Follows a running app's log for the errors nothing else can see. The
     * compiler answers for the source and the redefine answers for the bytes;
     * neither answers for a Spring context that failed to re-create a bean or a
     * call that landed on a stale proxy. Those exist only as lines in the app's
     * log, so an apply that never reads it can report {@code Stable} on an app
     * that is loudly broken.
     * <p>
     * On demand rather than on a thread: the file is the buffer, and a cursor
     * reads only what appeared since the last look, so a drain at apply time
     * and a drain at {@code status} time between them miss nothing.
     */
    static final class Watch {

        /** How long a drain keeps waiting for more after each new line. */
        private static final long QUIET_MILLIS = 100L;

        private final Cursor cursor;
        private final List<String> errors = new ArrayList<>();
        private int count;
        private String failure;
        private boolean continuing;

        Watch(Path log) {
            this.cursor = new Cursor(log);
        }

        /** New lines since the last look, errors among them recorded. */
        synchronized List<String> drain() {
            List<String> lines = cursor.drain();
            for (String line : lines) {
                boolean header = THROWN_HEADER.matcher(line).find();
                boolean logged = ERROR_LINE.matcher(line).find()
                        || DEV_SERVER_ERROR.matcher(line).find();
                if (logged) {
                    count++;
                    continuing = errors.size() < MAX_ERRORS;
                    if (continuing) {
                        errors.add(line);
                    }
                } else if (continuing && header) {
                    // The throwable the line above was about. Kept with it
                    // rather
                    // than counted again: one failure, reported as one error,
                    // and
                    // the half of it that names the type is this half.
                    int last = errors.size() - 1;
                    errors.set(last, errors.get(last) + " | " + line.strip());
                    continuing = false;
                } else {
                    continuing = false;
                }
                // Classified on the header too, not only on the logged line: a
                // linkage error's type never appears on the line carrying the
                // level, and Spring's real reason is in a "Caused by:" further
                // down still. Frames cannot match, so this stays narrow.
                if (failure == null && (logged || header)
                        && RELOAD_FAILURE.matcher(line).find()) {
                    failure = line.strip();
                }
            }
            return lines;
        }

        /**
         * The error in this window that means the change is not live, if any.
         * The apply that provoked it should escalate to a restart rather than
         * report {@code Stable}: a restart is the ground truth, and it either
         * clears the error or fails with the app's own words.
         */
        synchronized Optional<String> failure() {
            drain();
            return Optional.ofNullable(failure);
        }

        /**
         * Starts a fresh window. Whatever the log already holds belongs to what
         * came before, so it is read past and dropped rather than blamed on the
         * change about to be made.
         */
        synchronized void mark() {
            drain();
            errors.clear();
            count = 0;
            failure = null;
            continuing = false;
        }

        /**
         * The errors in this window, following the log for {@code millis}
         * first. Logging is asynchronous to the redefine that provoked it, so a
         * drain that returns the instant the connector replies can read an
         * empty log and call a broken app stable.
         * <p>
         * The whole window, with no early exit on a quiet poll: the errors
         * worth catching are logged by whichever thread the change reached - a
         * UI thread refreshing, a context re-creating a bean - and not by the
         * one that answered the redefine, so "the log went quiet for 100 ms" is
         * no evidence that it is finished. A fixed window is also a fixed cost,
         * which is what makes it something to tune rather than something to
         * wonder about.
         */
        synchronized List<String> settle(long millis) {
            long deadline = System.nanoTime() + millis * 1_000_000L;
            while (true) {
                drain();
                long remaining = (deadline - System.nanoTime()) / 1_000_000L;
                if (remaining <= 0) {
                    break;
                }
                try {
                    Thread.sleep(Math.min(QUIET_MILLIS, remaining));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return List.copyOf(errors);
        }

        /** The errors in this window, without waiting for more. */
        synchronized List<String> errors() {
            drain();
            return List.copyOf(errors);
        }

        /** How many there were, including the ones not kept. */
        synchronized int count() {
            drain();
            return count;
        }
    }

    /** The last lines of the log, blank ones dropped, oldest first. */
    static List<String> tail(Path log) {
        List<String> lines = window(log);
        return lines.size() <= TAIL_LINES ? lines
                : new ArrayList<>(
                        lines.subList(lines.size() - TAIL_LINES, lines.size()));
    }

    /**
     * The lines worth showing for a failure: the head of the last failure
     * report rather than the end of the log. A long trace pushes the reason out
     * of a fixed tail, which then carries all of the evidence and none of the
     * cause - twenty frames ending on {@code ... 46 common frames omitted}.
     */
    static List<String> excerpt(Path log) {
        List<String> lines = window(log);
        int start = reportStart(lines);
        return start < 0 ? tail(log)
                : new ArrayList<>(lines.subList(start,
                        Math.min(lines.size(), start + TAIL_LINES)));
    }

    /**
     * The one line that explains a failure, for the places with room for a
     * reason but not for a tail - {@code status} and a transaction's reason.
     */
    static Optional<String> cause(Path log) {
        List<String> lines = window(log);
        // The app's own diagnosis first: when Spring Boot knows why it could
        // not
        // start it says so in plain words, which beats any line guessed at
        // here.
        // The last such block, because an app that logged one of these and then
        // carried on did not fail for that reason.
        for (int i = lines.size() - 2; i >= 0; i--) {
            if (DESCRIPTION.matcher(lines.get(i)).matches()) {
                return Optional.of(lines.get(i + 1));
            }
        }
        int start = reportStart(lines);
        Optional<String> reason = reason(lines, Math.max(0, start));
        if (reason.isEmpty() && start >= 0) {
            // A report whose own lines name no exception - a bare ERROR from a
            // hotswap plugin, say - must not shadow a real trace above it, and
            // failing that it is still a better answer than the log's last
            // line,
            // which for an app that died quietly is whatever it did last.
            reason = reason(lines, 0).or(() -> Optional.of(lines.get(start)));
        }
        return reason.or(() -> lines.isEmpty() ? Optional.empty()
                : Optional.of(lines.get(lines.size() - 1)));
    }

    /**
     * The reason within one stretch of the log: the deepest {@code Caused by:},
     * else the exception that opened it. Deepest, because Spring wraps the real
     * reason in two or three bean-creation failures and only the innermost one
     * names the mistake - "No property 'findOne' found for type 'Task'".
     */
    private static Optional<String> reason(List<String> lines, int from) {
        for (int i = lines.size() - 1; i >= from; i--) {
            String line = lines.get(i);
            if (line.startsWith("Caused by:")) {
                // Without the prefix: this line goes on to be quoted inside a
                // sentence of the daemon's own ("restart: ...").
                return Optional
                        .of(line.substring("Caused by:".length()).trim());
            }
        }
        // Nothing wrapped it, so the report's first exception line is the
        // reason.
        // Forwards, because the ones after it are its frames.
        for (int i = from; i < lines.size(); i++) {
            if (THROWN.matcher(lines.get(i)).find()) {
                return Optional.of(lines.get(i));
            }
        }
        return Optional.empty();
    }

    /**
     * Where the last failure report begins, or -1 if the window holds none. The
     * last, not the first: the question is always about the latest failure.
     */
    private static int reportStart(List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (REPORT.matcher(lines.get(i)).find()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The tail of the log as lines, blank and banner-rule lines dropped. Wider
     * than what any caller prints: the reason for a failure and the lines worth
     * showing for it are found in here, not in the last twenty lines.
     */
    private static List<String> window(Path log) {
        long size = size(log);
        List<String> lines = new ArrayList<>();
        for (String line : read(log, Math.max(0, size - TAIL_BYTES)).lines()
                .toList()) {
            String trimmed = line.stripTrailing();
            // Spring Boot's banner rules around a failure report carry nothing
            // and would spend three of the lines available.
            if (!trimmed.isBlank() && !trimmed.matches("\\*{3,}")) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private static long size(Path log) {
        try {
            return Files.isRegularFile(log) ? Files.size(log) : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String read(Path log, long from) {
        if (!Files.isRegularFile(log)) {
            return "";
        }
        try (SeekableByteChannel channel = Files.newByteChannel(log,
                StandardOpenOption.READ)) {
            long available = channel.size() - from;
            if (available <= 0) {
                return "";
            }
            ByteBuffer buffer = ByteBuffer
                    .allocate((int) Math.min(available, TAIL_BYTES));
            channel.position(from);
            int read = channel.read(buffer);
            return read <= 0 ? "" : decode(buffer.array(), read);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * A read that starts at an arbitrary offset can begin mid-character, so
     * this replaces rather than fails: the text is only ever shown or matched.
     */
    private static String decode(byte[] bytes, int length) {
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }
}
