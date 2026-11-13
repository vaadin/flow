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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LockFileTest {

    @TempDir
    Path folder;

    @Test
    void acquire_concurrently_onlyOneHolderAtATime() throws Exception {
        Path path = folder.resolve("test.lock");
        int threads = 4;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger holders = new AtomicInteger();
        AtomicInteger maxHolders = new AtomicInteger();
        List<Thread> started = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(() -> {
                LockFile lock = new LockFile(path, "test");
                try {
                    start.await();
                    lock.acquire();
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
            thread.join();
        }

        assertEquals(1, maxHolders.get(),
                "Threads that find the lock free at the same time should still hold it one at a time");
    }
}
