/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A factory for creating daemon threads with custom naming conventions, used to
 * generate threads with a predefined naming pattern.
 * <p>
 * </p>
 * This class implements the {@link ThreadFactory} interface to provide a
 * mechanism for instantiating threads that are configured with specific
 * attributes such as name prefix, thread priority, and daemon status.
 *
 * @since 24.7.6
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
