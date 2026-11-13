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

    private static final String TAKEOVER_SUFFIX = ".takeover";

    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    /**
     * How long an unreadable lock file is given to get its content written
     * before it is considered abandoned, as creating the file and writing its
     * content are separate steps for the process taking the lock. Also how long
     * a takeover file may exist before it is considered abandoned.
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

            List<String> content;
            try {
                content = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (NoSuchFileException e) {
                // Released in the meantime
                continue;
            } catch (IOException e) {
                getLogger().debug("Could not read lock file ({})",
                        path.toAbsolutePath(), e);
                sleep();
                continue;
            }

            NodeTasksLockInfo lockInfo = parseLockInfo(content);
            if (lockInfo == null) {
                if (isOlderThanGracePeriod(path)) {
                    getLogger().warn(
                            "Removing the unreadable lock file ({}) of {}",
                            path.toAbsolutePath(), holderName);
                    removeAbandonedLock(content);
                } else {
                    // The holder has not yet written its content
                    sleep();
                }
            } else if (isRunning(lockInfo)) {
                if (!loggedWaiting) {
                    getLogger().info(
                            "Waiting for a previous instance of {} (pid: {}) to finish...",
                            holderName, lockInfo.pid());
                    loggedWaiting = true;
                }
                sleep();
            } else {
                // The process has died without removing the lock file
                removeAbandonedLock(content);
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

    /**
     * Removes the lock file, provided that it still has the content of the
     * abandoned lock.
     * <p>
     * Several waiters can find the same abandoned lock, and one of them may
     * already have removed it and taken the lock by the time another gets to
     * remove it. Removing happens one waiter at a time, under a takeover file,
     * and only after checking the content again, so that the lock just taken by
     * someone else is never removed.
     */
    private void removeAbandonedLock(List<String> abandonedContent) {
        Path takeover = path
                .resolveSibling(path.getFileName() + TAKEOVER_SUFFIX);
        try {
            Files.createFile(takeover);
        } catch (FileAlreadyExistsException e) {
            // Left behind by a waiter that died while removing a lock
            if (isOlderThanGracePeriod(takeover)) {
                FileIOUtils.deleteQuietly(takeover);
            } else {
                sleep();
            }
            return;
        } catch (IOException e) {
            getLogger().error("Error removing the abandoned lock file ({})",
                    path.toAbsolutePath(), e);
            sleep();
            return;
        }
        try {
            if (abandonedContent
                    .equals(Files.readAllLines(path, StandardCharsets.UTF_8))) {
                Files.delete(path);
            }
        } catch (NoSuchFileException e) {
            // Released in the meantime
        } catch (IOException e) {
            getLogger().error("Error removing the abandoned lock file ({})",
                    path.toAbsolutePath(), e);
        } finally {
            FileIOUtils.deleteQuietly(takeover);
        }
    }

    private static boolean isRunning(NodeTasksLockInfo lockInfo) {
        return ProcessHandle.of(lockInfo.pid())
                .map(process -> normalizeCommandLine(process.info())
                        .equals(lockInfo.commandLine()))
                .orElse(false);
    }

    private static boolean isOlderThanGracePeriod(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant()
                    .isBefore(Instant.now().minus(UNREADABLE_GRACE_PERIOD));
        } catch (IOException e) {
            return false;
        }
    }

    private NodeTasksLockInfo readLockFile() throws IOException {
        NodeTasksLockInfo lockInfo = parseLockInfo(
                Files.readAllLines(path, StandardCharsets.UTF_8));
        if (lockInfo == null) {
            throw new IllegalStateException("Invalid lock file content");
        }
        return lockInfo;
    }

    private static NodeTasksLockInfo parseLockInfo(List<String> lines) {
        if (lines.size() != 2) {
            return null;
        }
        try {
            return new NodeTasksLockInfo(Long.parseLong(lines.get(0)),
                    lines.get(1));
        } catch (NumberFormatException e) {
            return null;
        }
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
