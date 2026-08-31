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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
 */
final class Handshake {

    static final String FILE_NAME = "daemon.properties";

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
