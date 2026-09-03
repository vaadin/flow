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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Properties;

/**
 * The discovery file under {@code .vaadin/} that lets any client find the one
 * daemon for this project root.
 * <p>
 * Deliberately a properties file rather than JSON: the CLI is a bash script and
 * needs to read the port and token without a JSON parser or a JVM start. It
 * carries the process start time alongside the pid so a recycled pid cannot be
 * mistaken for a live daemon.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class Handshake {

    static final String FILE_NAME = "daemon.properties";

    static final String LOCK_NAME = "daemon.lock";

    final long pid;
    final long startEpochMillis;
    final int port;
    final String token;
    final String projectRoot;

    Handshake(long pid, long startEpochMillis, int port, String token,
            String projectRoot) {
        this.pid = pid;
        this.startEpochMillis = startEpochMillis;
        this.port = port;
        this.token = token;
        this.projectRoot = projectRoot;
    }

    static Path path(Path projectRoot) {
        return projectRoot.resolve(".vaadin").resolve(FILE_NAME);
    }

    static Path lockPath(Path projectRoot) {
        return projectRoot.resolve(".vaadin").resolve(LOCK_NAME);
    }

    /**
     * The right to be this project's daemon, held as long as the daemon runs.
     * <p>
     * The record below cannot decide this on its own. Reading it, seeing
     * nothing, binding a port and writing it is three steps, and two
     * simultaneous first invocations - an agent and a developer, or two agents
     * - can both take the first step before either takes the last. Both would
     * then start a daemon, the second record would overwrite the first, and
     * whichever daemon each client reached would try to launch the application
     * on the same port.
     * <p>
     * An OS file lock is what makes it one step: exactly one process holds it,
     * and it is released by the kernel if that process dies, so a crashed
     * daemon leaves nothing to reap.
     */
    static final class Claim implements AutoCloseable {

        private final FileChannel channel;

        private final FileLock lock;

        private Claim(FileChannel channel, FileLock lock) {
            this.channel = channel;
            this.lock = lock;
        }

        @Override
        public void close() {
            try {
                lock.release();
            } catch (IOException ignored) {
                // Releasing is best effort: closing the channel below, and
                // process exit after it, release the lock anyway.
            }
            try {
                channel.close();
            } catch (IOException ignored) {
                // As above - nothing a shutting-down daemon can act on.
            }
        }
    }

    /**
     * Claims the project's daemon slot, or reports empty when another process
     * already holds it.
     * <p>
     * The lock file is left behind on purpose: on Windows it cannot be deleted
     * while a handle is open, and an empty file is not state - the lock, not
     * the file, is what is held.
     */
    static Optional<Claim> claim(Path projectRoot) throws IOException {
        Path file = lockPath(projectRoot);
        Files.createDirectories(file.getParent());
        FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                return Optional.empty();
            }
            return Optional.of(new Claim(channel, lock));
        } catch (OverlappingFileLockException e) {
            // Another thread of this same JVM holds it. Only reachable in a
            // test, and the answer is the same one another process gets.
            channel.close();
            return Optional.empty();
        } catch (IOException e) {
            channel.close();
            throw e;
        }
    }

    /** Written temp-then-move so a reader never sees a half-written file. */
    void write(Path projectRoot) throws IOException {
        Path target = path(projectRoot);
        Files.createDirectories(target.getParent());
        Properties props = new Properties();
        props.setProperty("pid", Long.toString(pid));
        props.setProperty("startEpochMillis", Long.toString(startEpochMillis));
        props.setProperty("port", Integer.toString(port));
        props.setProperty("token", token);
        props.setProperty("projectRoot", this.projectRoot);
        StringBuilder sb = new StringBuilder();
        for (String name : props.stringPropertyNames().stream().sorted()
                .toList()) {
            sb.append(name).append('=').append(escape(props.getProperty(name)))
                    .append('\n');
        }
        Path temp = target.resolveSibling(FILE_NAME + ".tmp");
        Files.writeString(temp, sb.toString(), StandardCharsets.UTF_8);
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * A value as the properties format needs it.
     * <p>
     * The lines are written by hand - sorted, so the file is stable enough to
     * diff - but they are read back with {@link Properties#load}, which treats
     * a backslash as an escape. Without this, a Windows project root comes back
     * as {@code C:UsersdevTemp}: the token is Base64url and survives, the path
     * does not.
     */
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n")
                .replace("\r", "\\r").replace("\t", "\\t");
    }

    static Optional<Handshake> read(Path projectRoot) {
        Path file = path(projectRoot);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (BufferedReader in = Files.newBufferedReader(file,
                StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(in);
            return Optional.of(new Handshake(
                    Long.parseLong(props.getProperty("pid", "-1")),
                    Long.parseLong(props.getProperty("startEpochMillis", "-1")),
                    Integer.parseInt(props.getProperty("port", "-1")),
                    props.getProperty("token", ""),
                    props.getProperty("projectRoot", "")));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    static void delete(Path projectRoot) {
        try {
            Files.deleteIfExists(path(projectRoot));
        } catch (IOException ignored) {
            // Best effort; a stale file is reaped again on the next start.
        }
    }

    /**
     * Process-level liveness, as the design requires — never a socket probe.
     * The recorded start time guards against pid reuse: a live process with a
     * different start instant is a different process.
     */
    boolean isProcessAlive() {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty() || !handle.get().isAlive()) {
            return false;
        }
        if (startEpochMillis <= 0) {
            return true;
        }
        return handle.get().info().startInstant()
                .map(instant -> Math
                        .abs(instant.toEpochMilli() - startEpochMillis) < 2000)
                .orElse(true);
    }
}
