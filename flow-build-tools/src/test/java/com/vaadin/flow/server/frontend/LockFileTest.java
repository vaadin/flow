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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class LockFileTest {

    @TempDir
    Path folder;

    @Test
    void acquire_concurrentlyWithLockOfDeadProcess_onlyOneHolderAtATime()
            throws Exception {
        Path path = folder.resolve("test.lock");
        // All threads find this lock abandoned and try to take it over
        Files.writeString(path, "1\nnot the command line of the process",
                StandardCharsets.UTF_8);
        int threads = 4;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger holders = new AtomicInteger();
        AtomicInteger maxHolders = new AtomicInteger();
        AtomicInteger acquisitions = new AtomicInteger();
        List<Thread> started = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(() -> {
                LockFile lock = new LockFile(path, "test");
                try {
                    start.await();
                    lock.acquire();
                    acquisitions.incrementAndGet();
                    try {
                        maxHolders.accumulateAndGet(holders.incrementAndGet(),
                                Math::max);
                        Thread.sleep(50);
                        holders.decrementAndGet();
                    } finally {
                        lock.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            thread.start();
            started.add(thread);
        }

        start.countDown();
        for (Thread thread : started) {
            thread.join(TimeUnit.SECONDS.toMillis(30));
        }

        assertEquals(threads, acquisitions.get(),
                "Every thread should get the lock in turn");
        assertEquals(1, maxHolders.get(),
                "Threads that find the lock free or abandoned at the same time should still hold it one at a time");
    }

    @Test
    void acquire_unreadableLockFile_isTakenOverWhenOld() throws Exception {
        Path path = folder.resolve("test.lock");
        Files.writeString(path, "unreadable", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(path,
                FileTime.from(Instant.now().minus(Duration.ofMinutes(1))));

        assertTimeoutPreemptively(Duration.ofSeconds(30),
                () -> new LockFile(path, "test").acquire());

        assertEquals(Long.toString(ProcessHandle.current().pid()),
                Files.readAllLines(path, StandardCharsets.UTF_8).get(0));
    }
}
