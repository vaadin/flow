/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.io.Serializable;

/**
 * Webpack development server port wrapper. The port is stored into
 * {@link com.vaadin.flow.server.VaadinContext} using the class as identifier.
 */
public class DevModePort implements Serializable {

    private final int port;

    /**
     * Creates the dev mode port.
     *
     * @param port
     *            the value of the port.
     */
    public DevModePort(int port) {
        this.port = port;
    }

    /**
     * Gets the port number.
     *
     * @return port number.
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "" + port;
    }

    @Override
    public int hashCode() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DevModePort) {
            DevModePort devModePort = (DevModePort) obj;
            return devModePort.port == this.port;
        }

        return false;
    }
}
