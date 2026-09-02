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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Opens the dev loop's end of the connection to the daemon, when there is a
 * daemon to connect to.
 * <p>
 * Everything else the dev loop needs in the application - the hotswapper, the
 * atomic redefine, the resource push - is reached from the registration
 * connection, so this is the whole of its startup path. An application the
 * daemon did not launch is left entirely alone.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class DevLoopInitListener implements VaadinServiceInitListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DevLoopInitListener.class);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        if (!DevLoopRegistration.isDaemonLaunched()) {
            LOGGER.debug(
                    "Not launched by the vaadin-dev daemon; skipping dev-loop registration");
            return;
        }
        if (event.getSource().getDeploymentConfiguration().isProductionMode()) {
            // The daemon only ever launches an app in dev mode, so this means
            // the configuration and the launch disagree - worth a word rather
            // than a connection that would never be able to redefine anything.
            LOGGER.warn(
                    "The vaadin-dev daemon launched this application but it is running in production mode; the dev loop is disabled");
            return;
        }
        try {
            DevLoopRegistration.start(event.getSource());
        } catch (RuntimeException e) {
            // The loop is a convenience; the application is not. A context that
            // a hotswapper cannot be built for must cost the dev loop and
            // nothing else.
            LOGGER.warn(
                    "Could not start the dev-loop registration; vaadin-dev will not be able to apply changes",
                    e);
        }
    }
}
