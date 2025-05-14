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
package com.vaadin.flow.internal;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Utility class for network related methods.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
