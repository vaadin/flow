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
package com.vaadin.base.devserver.devloop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.PublicResourcesLiveUpdater;
import com.vaadin.base.devserver.ThemeLiveUpdater;
import com.vaadin.base.devserver.hotswap.Hotswapper;
import com.vaadin.flow.server.VaadinService;

/**
 * Registers the running application with the dev-loop daemon over a connection
 * held open for the application's lifetime.
 * <p>
 * The connection is the liveness signal in both directions: the daemon learns
 * the app is up when it arrives and that the app is gone when it closes, with
 * no polling and no port probing. The daemon passes its port and token in as
 * system properties at launch, so there is nothing to discover and no file to
 * read.
 * <p>
 * If those properties are absent the app was not launched by the daemon (a
 * developer running it from an IDE, say) and nothing here happens - the
 * ownership model says the daemon aggregates state, it never competes for it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class DevLoopRegistration {

    static final String DAEMON_PORT_PROPERTY = "vaadin.devloop.daemonPort";
    static final String TOKEN_PROPERTY = "vaadin.devloop.token";

    /** Whitespace and C0 controls, which is everything that breaks a line. */
    private static final Pattern CONTROL_OR_SPACE_RUN = Pattern
            .compile("[\\s\\p{Cntrl}]+");

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DevLoopRegistration.class);

    /**
     * The dev-mode service, for the frontend and resource legs. Static because
     * the daemon owns this JVM and serves exactly one application in it.
     */
    private static volatile VaadinService service;

    private DevLoopRegistration() {
    }

    /**
     * Whether the daemon launched this JVM, which is the only case in which any
     * of this applies.
     *
     * @return {@code true} if the daemon's handshake properties are present
     */
    static boolean isDaemonLaunched() {
        return System.getProperty(DAEMON_PORT_PROPERTY) != null
                && System.getProperty(TOKEN_PROPERTY) != null;
    }

    /**
     * Opens the registration connection and hands the resource leg over from
     * Flow's own CSS watcher.
     *
     * @param vaadinService
     *            the dev-mode service
     */
    static void start(VaadinService vaadinService) {
        service = vaadinService;
        int port = Integer.parseInt(System.getProperty(DAEMON_PORT_PROPERTY));
        String token = System.getProperty(TOKEN_PROPERTY);

        // The daemon drives Flow's hotswapper over the registration connection,
        // so the connector registers it here rather than relying on anything
        // else to have done it. Nothing else in Flow does, and nothing should:
        // onHotswap is an instance method, so a tool that wants to drive it has
        // to obtain the instance, and register is the only way to get one. That
        // also means registering for applications that have no such tool would
        // be pure cost - it instantiates every VaadinHotswapper, one of which
        // installs an after-navigation listener that walks the component tree.
        //
        // Idempotent, so a hotswap agent that injects its own call still ends
        // up
        // with one instance rather than two doubling every refresh.
        if (Hotswapper.register(vaadinService).isEmpty()) {
            LOGGER.warn(
                    "Flow's hotswapper is not available, so an apply can only ever restart the application");
        }

        // Flow's watcher fires on save, not on apply. The whole point of the
        // transaction model is that one command decides when a change goes
        // live, and a second watcher pushing on its own makes "what is the
        // state of my last change?" unanswerable again.
        PublicResourcesLiveUpdater.suspend(vaadinService.getContext());
        ThemeLiveUpdater.suspend(vaadinService.getContext());

        String mode = modeOf(vaadinService);

        Thread thread = new Thread(() -> hold(port, token, mode),
                "devloop-registration");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Which dev mode the app is actually running in.
     * <p>
     * Not {@code isDevModeLiveReloadEnabled()}, which reads as if it answered
     * this and does not: it is
     * {@code isDevToolsEnabled() && devmode.liveReload} and both default to
     * true, so it says {@code DEVELOPMENT_FRONTEND_LIVERELOAD} for a dev-bundle
     * application as well. The daemon uses this to decide whether a frontend
     * edit was already applied by Vite or needs the bundle rebuilt, and those
     * are opposite answers.
     *
     * @param vaadinService
     *            the dev-mode service
     * @return the mode's name
     */
    static String modeOf(VaadinService vaadinService) {
        return vaadinService.getDeploymentConfiguration().getMode().name();
    }

    /**
     * The service the daemon launched, or {@code null} before registration.
     *
     * @return the dev-mode service, or {@code null}
     */
    static VaadinService service() {
        return service;
    }

    /**
     * The hotswapper registered for this service.
     * <p>
     * Looked up on demand rather than captured in {@link #start}, so a hotswap
     * agent that registered one first is found too.
     *
     * @return the registered hotswapper, or empty when there is none
     */
    static Optional<Hotswapper> hotswapper() {
        VaadinService current = service;
        return current == null ? Optional.empty()
                : Hotswapper.getRegistered(current);
    }

    private static void hold(int port, String token, String mode) {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(),
                        true, StandardCharsets.UTF_8);
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.UTF_8))) {
            socket.setKeepAlive(true);
            out.println(token + " register " + mode + " "
                    + ProcessHandle.current().pid());
            LOGGER.info("Registered with the dev-loop daemon on port {} ({})",
                    port, oneLine(in.readLine()));
            // Block for the app's lifetime, serving commands on the same
            // connection. Reaching the end of the stream means the daemon went
            // away; the JVM exiting closes this socket, which is how the daemon
            // learns the app is gone.
            String request;
            while ((request = in.readLine()) != null) {
                out.println(handle(request.trim()));
            }
            LOGGER.info("The dev-loop daemon closed the registration");
        } catch (Exception e) {
            LOGGER.info("The dev-loop registration ended: {}", e.toString());
            LOGGER.debug("The dev-loop registration ended", e);
        }
    }

    /**
     * Commands the daemon issues over the registration connection. Always
     * answers exactly one line, even on failure, so the daemon never blocks
     * waiting for a reply that is not coming.
     */
    private static String handle(String request) {
        try {
            if (request.startsWith("REDEFINE ")) {
                return DevLoopRedefiner.redefine(
                        request.substring("REDEFINE ".length()).trim());
            }
            if (request.startsWith("RESOURCES ")) {
                return DevLoopRedefiner.resources(
                        request.substring("RESOURCES ".length()).trim());
            }
            if (request.startsWith("THEME ")) {
                return DevLoopRedefiner
                        .theme(request.substring("THEME ".length()).trim());
            }
            if (request.equals("RELOAD")) {
                return DevLoopRedefiner.reload();
            }
            if (request.equals("INFO")) {
                return DevLoopRedefiner.info();
            }
            // The daemon's own answer for the frontend folder rides in as the
            // argument rather than as a reply field: a reply is parsed on
            // whitespace and a Windows path can contain a space.
            if (request.startsWith("FRONTEND ")) {
                return DevLoopRedefiner.frontend(
                        request.substring("FRONTEND ".length()).trim());
            }
            if (request.equals("FRONTEND")) {
                return DevLoopRedefiner.frontend(null);
            }
            // The paths ride in as the argument for the same reason the
            // frontend folder does above: a reply is parsed on whitespace and
            // a Windows path can contain a space.
            if (request.startsWith("FRONTEND_CHECK ")) {
                return DevLoopRedefiner.frontendCheck(
                        request.substring("FRONTEND_CHECK ".length()).trim());
            }
            if (request.equals("PING")) {
                return "OK pong";
            }
            return "ERR kind=protocol message=unknown-request";
        } catch (Throwable t) {
            return "ERR kind=internal class=" + t.getClass().getName()
                    + " message=" + oneLine(t.getMessage());
        }
    }

    /**
     * One log- and protocol-safe line. Both directions of this socket end up in
     * a log or in a single-line reply, so a value that carries a line break -
     * or any other control character - would split one record into two.
     */
    private static String oneLine(String value) {
        return value == null ? "null"
                : CONTROL_OR_SPACE_RUN.matcher(value).replaceAll(" ").trim();
    }
}
