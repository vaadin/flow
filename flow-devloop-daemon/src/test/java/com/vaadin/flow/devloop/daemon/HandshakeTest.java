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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The handshake file is what every client uses to find the one daemon for a
 * project, and a stale one is what makes a second daemon fight the first for
 * port 8080.
 */
class HandshakeTest {

    @TempDir
    private Path root;

    @Test
    void writeThenRead_roundTripsEveryField() throws IOException {
        new Handshake(4242, 1_700_000_000_000L, 51_234, "s3cr3t",
                root.toString()).write(root);

        Optional<Handshake> read = Handshake.read(root);

        assertTrue(read.isPresent());
        assertEquals(4242, read.get().pid);
        assertEquals(1_700_000_000_000L, read.get().startEpochMillis);
        assertEquals(51_234, read.get().port);
        assertEquals("s3cr3t", read.get().token);
        assertEquals(root.toString(), read.get().projectRoot);
    }

    @Test
    void write_landsUnderDotVaadinAndLeavesNoTempFile() throws IOException {
        new Handshake(1, 1, 1, "t", root.toString()).write(root);

        // Outside target/, deliberately: mvn clean must not orphan a running
        // daemon by deleting the only record of it.
        assertTrue(Files.isRegularFile(
                root.resolve(".vaadin").resolve("daemon.properties")));
        assertFalse(Files.exists(
                root.resolve(".vaadin").resolve("daemon.properties.tmp")));
    }

    @Test
    void claim_isHeldByOneProcessAtATime() throws IOException {
        // Two first invocations at once would otherwise both read no record,
        // both bind a port and both start the application.
        Optional<Handshake.Claim> first = Handshake.claim(root);
        assertTrue(first.isPresent());

        assertTrue(Handshake.claim(root).isEmpty());

        first.get().close();
        Optional<Handshake.Claim> after = Handshake.claim(root);
        assertTrue(after.isPresent(), "a released claim is available again");
        after.get().close();
    }

    @Test
    void read_noFile_isEmpty() {
        assertTrue(Handshake.read(root).isEmpty());
    }

    @Test
    void read_unparseableFile_isEmptyRatherThanThrowing() throws IOException {
        Path file = Handshake.path(root);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "port=not-a-number\n");

        assertTrue(Handshake.read(root).isEmpty());
    }

    @Test
    void isProcessAlive_ownPid_isAlive() {
        long pid = ProcessHandle.current().pid();
        long start = ProcessHandle.current().info().startInstant()
                .map(Instant::toEpochMilli).orElse(0L);

        assertTrue(new Handshake(pid, start, 1, "t", root.toString())
                .isProcessAlive());
    }

    @Test
    void isProcessAlive_recycledPid_isNotMistakenForTheOriginal() {
        // Same pid, a start time it cannot have had: a recycled pid is a
        // different process, and reaping is what stops a second daemon.
        long pid = ProcessHandle.current().pid();

        assertFalse(new Handshake(pid, 1_000L, 1, "t", root.toString())
                .isProcessAlive());
    }

    @Test
    void delete_removesTheRecord() throws IOException {
        new Handshake(1, 1, 1, "t", root.toString()).write(root);

        Handshake.delete(root);

        assertTrue(Handshake.read(root).isEmpty());
    }
}
