/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens a server socket which is supposed to be opened until dev mode is active
 * inside JVM.
 * <p>
 * If this socket is closed then there is no anymore Java "client" for the
 * webpack dev server and it should be stopped.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
                    enterReloadMessageReadLoop(accept);
                } catch (IOException e) {
                    getLogger().debug(
                            "Error occurred during accept a connection", e);
                }
            }
        }

        void stop() {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    getLogger().debug(
                            "Error occurred during close the server socket", e);
                }
            }
        }

        private Logger getLogger() {
            return LoggerFactory.getLogger(WatchDogServer.class);
        }

        private void enterReloadMessageReadLoop(Socket accept)
                throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    accept.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                DevModeHandler devModeHandler = DevModeHandler
                        .getDevModeHandler();
                if ("reload".equals(line) && devModeHandler != null
                        && devModeHandler.getLiveReload() != null) {
                    devModeHandler.getLiveReload().reload();
                }
            }
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