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
package com.vaadin.flow.spring.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaadinDefaultAsyncInitFutureRegistry
        implements VaadinAsyncInitFutureRegistry, AutoCloseable {
    private static final Logger LOG = LoggerFactory
            .getLogger(VaadinDefaultAsyncInitFutureRegistry.class);

    protected List<CompletableFuture<?>> cfs = new ArrayList<>();
    protected ReentrantLock lock = new ReentrantLock();

    @Override
    public void register(final CompletableFuture<Void> cf) {
        lock.lock();
        try {
            throwIfNotUsable();

            cfs.add(cf);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void waitForAllUntilFinishedAndClose() {
        lock.lock();
        try {
            throwIfNotUsable();

            final long startMs = System.currentTimeMillis();

            CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new))
                    .join();

            LOG.debug("Waiting for async init({}x futures) took {}ms",
                    this.cfs.size(), System.currentTimeMillis() - startMs);

            close();
        } finally {
            lock.unlock();
        }
    }

    protected void throwIfNotUsable() {
        if (cfs == null) {
            throw new IllegalStateException(
                    "Registry was closed and is no longer usable");
        }
    }

    @Override
    public void close() {
        cfs = null;
    }
}
