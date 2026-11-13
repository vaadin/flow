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
package com.vaadin.flow.server.frontend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.server.frontend.NodeTasks.NodeTasksLockInfo;

/**
 * A lock held through a file that names the process holding it, so that
 * frontend build steps sharing a folder run one at a time across threads and
 * build processes.
 * <p>
 * A lock left behind by a process that no longer runs is taken over.
 */
class LockFile {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    /**
     * How long an unreadable lock file is given to get its content written
     * before it is considered broken, as creating the file and writing its
     * content are separate steps for the process taking the lock.
     */
    private static final Duration UNREADABLE_GRACE_PERIOD = Duration
            .ofSeconds(5);

    private final Path path;
    private final String holderName;

    /**
     * Creates a lock held through the given file.
     *
     * @param path
     *            the lock file, in a folder that exists
     * @param holderName
     *            what holds the lock, used in log messages
     */
    LockFile(Path path, String holderName) {
        this.path = path;
        this.holderName = holderName;
    }

    /**
     * Takes the lock, waiting for as long as a running process holds it.
     * <p>
     * When the lock file cannot be written at all, this logs the problem and
     * returns without holding the lock rather than failing the build.
     */
    void acquire() {
        boolean loggedWaiting = false;
        while (true) {
            try {
                writeLockFile();
                return;
            } catch (FileAlreadyExistsException e) {
                // Held by someone else, see below whether to wait
            } catch (IOException e) {
                getLogger().error("Error writing lock file ({})",
                        path.toAbsolutePath(), e);
                return;
            }

            NodeTasksLockInfo lockInfo;
            try {
                lockInfo = readLockFile();
            } catch (NoSuchFileException e) {
                // Released in the meantime
                continue;
            } catch (Exception e) {
                if (isRecentlyModified()) {
                    // The holder has not yet written its content
                    sleep();
                } else {
                    getLogger().error(
                            "Removing the unreadable lock file ({}) of {}",
                            path.toAbsolutePath(), holderName, e);
                    FileIOUtils.deleteQuietly(path);
                }
                continue;
            }

            Optional<ProcessHandle> processHandle = ProcessHandle
                    .of(lockInfo.pid());
            if (processHandle.isPresent()
                    && normalizeCommandLine(processHandle.get().info())
                            .equals(lockInfo.commandLine())) {
                if (!loggedWaiting) {
                    getLogger().info(
                            "Waiting for a previous instance of {} (pid: {}) to finish...",
                            holderName, lockInfo.pid());
                    loggedWaiting = true;
                }
                sleep();
            } else {
                // The process has died without removing the lock file
                FileIOUtils.deleteQuietly(path);
            }
        }
    }

    /**
     * Releases the lock, provided that this process holds it.
     */
    void release() {
        if (!Files.exists(path)) {
            getLogger().warn("Somebody else has removed the lock file ({})",
                    path.toAbsolutePath());
            return;
        }

        try {
            long pid = readLockFile().pid();
            if (pid != ProcessHandle.current().pid()) {
                getLogger().warn(
                        "Another process ({}) has overwritten the lock file ({})",
                        pid, path.toAbsolutePath());
                return;
            }
            FileIOUtils.deleteQuietly(path);
        } catch (Exception e) {
            getLogger().error("Error releasing lock file ({})",
                    path.toAbsolutePath());
        }
    }

    private void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for another "
                    + holderName + " to finish", e);
        }
    }

    private boolean isRecentlyModified() {
        try {
            return Files.getLastModifiedTime(path).toInstant()
                    .isAfter(Instant.now().minus(UNREADABLE_GRACE_PERIOD));
        } catch (IOException e) {
            return false;
        }
    }

    private NodeTasksLockInfo readLockFile()
            throws NumberFormatException, IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 2) {
            throw new IllegalStateException(
                    "Invalid lock file. It should contain 2 rows but contains "
                            + lines);
        }
        return new NodeTasksLockInfo(Long.parseLong(lines.get(0)),
                lines.get(1));
    }

    /**
     * Creates the lock file, failing if it exists, so that of several processes
     * or threads seeing the lock as free only one gets it.
     */
    private void writeLockFile() throws IOException {
        ProcessHandle currentProcess = ProcessHandle.current();
        String commandLine = normalizeCommandLine(currentProcess.info());
        List<String> lines = List.of(Long.toString(currentProcess.pid()),
                commandLine);
        Files.write(path, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static String normalizeCommandLine(ProcessHandle.Info processInfo) {
        return processInfo.commandLine()
                .map(line -> line.replaceAll("\\r?\\n", " \\\\n")).orElse("");
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(LockFile.class);
    }
}
