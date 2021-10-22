/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.vaadin.flow.server.frontend.FrontendUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the output of a dev server and scans for given success and/or failure
 * patterns while copying the dev server output to standard output.
 * <p>
 * Triggers an event whenever a success or failure pattern is found on a row.
 */
public class DevServerOutputFinder {

    private static class Finder implements Runnable {

        private InputStream inputStream;
        private StringBuilder cumulativeOutput = new StringBuilder();
        private Pattern success;
        private Pattern failure;
        private Consumer<Result> onMatch;

        private Finder(InputStream inputStream, Pattern success,
                Pattern failure, Consumer<Result> onMatch) {
            this.inputStream = inputStream;
            this.success = success;
            this.failure = failure;
            this.onMatch = onMatch;
        }

        @Override
        public void run() {
            InputStreamReader reader = new InputStreamReader(inputStream,
                    StandardCharsets.UTF_8);
            try {
                readLinesLoop(reader);
            } catch (IOException e) {
                getLogger().error("Exception when reading stream.", e);
                onMatch.accept(new Result(false));
            }

            // Process closed stream, means that it exited, notify
            // DevModeHandler to continue without any result
            onMatch.accept(null);
        }

        private void readLinesLoop(InputStreamReader reader)
                throws IOException {
            StringBuilder line = new StringBuilder();
            for (int i; (i = reader.read()) >= 0;) {
                char ch = (char) i;
                FrontendUtils.console("%c", ch);
                line.append(ch);
                if (ch == '\n') {
                    processLine(line.toString());
                    line.setLength(0);
                }
            }
        }

        private void processLine(String line) {
            // skip webpack progress lines
            if (line.contains("\b")) {
                return;
            }

            // remove color escape codes for console
            String cleanLine = line.replaceAll("(\u001b\\[[;\\d]*m|[\b\r]+)",
                    "");

            boolean succeed = success != null && success.matcher(line).find();
            boolean failed = failure != null && failure.matcher(line).find();

            // save output so as it can be used to alert user in browser, unless
            // it's `: Failed to compile.`
            if (!failed) {
                cumulativeOutput.append(cleanLine);
            }

            // We found the success or failure pattern in stream
            if (succeed || failed) {
                onMatch.accept(
                        new Result(succeed, cumulativeOutput.toString()));
                cumulativeOutput = new StringBuilder();
            }
        }

        private static Logger getLogger() {
            return LoggerFactory.getLogger(Finder.class);
        }

    }

    /**
     * Encapsulates the result of a find operation.
     */
    public static class Result {

        private boolean success;
        private String output;

        private Result(boolean success) {
            this(success, "");
        }

        private Result(boolean success, String output) {
            this.success = success;
            this.output = output;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }
    }

    private CountDownLatch monitor;
    private Finder finder;

    /**
     * Creates a new finder that scans for the given success and/or failure
     * pattern.
     * 
     * @param inputStream
     *            the stream to scan
     * @param success
     *            the pattern indicating success
     * @param failure
     *            the pattern indicating failure
     * @param onMatch
     *            callback triggered when either success or failure is found
     */
    public DevServerOutputFinder(InputStream inputStream, Pattern success,
            Pattern failure, Consumer<Result> onMatch) {
        monitor = new CountDownLatch(1);
        finder = new Finder(inputStream, success, failure, result -> {
            if (result != null) {
                onMatch.accept(result);
            }
            monitor.countDown();
        });

    }

    /**
     * Runs the find operation.
     */
    public void find() {
        Thread finderThread = new Thread(finder);
        finderThread.setDaemon(true);
        finderThread.setName("dev-server-output-reader");
        finderThread.start();
    }

    /**
     * Blocks until the first match is found and the callback has been run.
     * 
     * @param timeoutInSeconds
     *            the maximum number of seconds to wait
     * @throws InterruptedException
     *             if the finder thread is interrupted
     * @return {@code true} if a match was found, {@code false} if a timeout
     *         occurred
     */
    public boolean awaitFirstMatch(int timeoutInSeconds)
            throws InterruptedException {
        return monitor.await(timeoutInSeconds, TimeUnit.SECONDS);
    }

}
