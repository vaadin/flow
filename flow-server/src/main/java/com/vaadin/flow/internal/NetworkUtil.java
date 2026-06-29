/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Utility class for network related methods.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.0
 */
public class NetworkUtil {

    private NetworkUtil() {
        // Utils only
    }

    /**
     * Returns an available tcp port in the system.
     *
     * @return a port number which is not busy
     */
    public static int getFreePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to find a free port", e);
        }
    }

    /**
     * Checks if the given port is free.
     *
     * @param port
     *            the port to check
     * @return true if the port is free, false otherwise
     */
    public static boolean isFreePort(int port) {
        try (ServerSocket s = new ServerSocket(port)) {
            s.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }

    }
}
