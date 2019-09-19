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
package com.vaadin.flow.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens a server socket which is supposed to be opened until dev mode is active
 * inside JVM.
 * <p>
 * If this socket is closed then there is no anymore Java "client" for the
 * webpack dev server and it should be stopped.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
class DevServerWatchDog {

    private static class WatchDogServer implements Runnable {

        private final ServerSocket server;

        WatchDogServer() {
            try {
                server = new ServerSocket(0);
                server.setSoTimeout(0);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Watchdog server has started on port {}",
                            server.getLocalPort());
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not open a server socket", e);
            }
        }

        @Override
        public void run() {
            while (!server.isClosed()) {
                try {
                    Socket accept = server.accept();
                    accept.setSoTimeout(0);
                } catch (IOException e) {
                    getLogger().debug(
                            "Error occured during accept a connection", e);
                }
            }
        }

        void stop() {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    getLogger().debug(
                            "Error occured during close the server socket", e);
                }
            }
        }

        private Logger getLogger() {
            return LoggerFactory.getLogger(WatchDogServer.class);
        }

    }

    private final WatchDogServer watchDogServer;

    DevServerWatchDog() {
        watchDogServer = new WatchDogServer();

        Thread serverThread = new Thread(watchDogServer);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    int getWatchDogPort() {
        return watchDogServer.server.getLocalPort();
    }

    void stop() {
        watchDogServer.stop();
    }
}
