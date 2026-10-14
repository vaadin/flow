/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

/**
 * Shutdown hook for tests. Supports just one listener.
 */
public class ShutdownHook {

    public static Runnable shutdownHook;

    public static void registerShutdownHook(Runnable shutdownHook) {
        ShutdownHook.shutdownHook = shutdownHook;
    }
}
