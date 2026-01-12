/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A factory for creating daemon threads with custom naming conventions, used to
 * generate threads with a predefined naming pattern.
 * <p>
 *
 * This class implements the {@link ThreadFactory} interface to provide a
 * mechanism for instantiating threads that are configured with specific
 * attributes such as name prefix, thread priority, and daemon status.
 */
public class NamedDaemonThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private final String namePrefix;

    /**
     * Constructs a new {@code NamedDaemonThreadFactory} with the specified name
     * prefix for the threads created by this factory.
     *
     * @param namePrefix
     *            the prefix to be used for naming threads created by this
     *            factory, not {@literal null}.
     */
    public NamedDaemonThreadFactory(String namePrefix) {
        this.namePrefix = Objects.requireNonNull(namePrefix,
                "namePrefix must not be null");
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String threadName = namePrefix + "-" + threadNumber.getAndIncrement();
        Thread thread = new Thread(runnable, threadName);
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}
