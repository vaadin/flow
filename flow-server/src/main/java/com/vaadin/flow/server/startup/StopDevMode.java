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

package com.vaadin.flow.server.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.server.DevModeHandler;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT;
import static com.vaadin.flow.server.Constants.VAADIN_PREFIX;

/**
 * Stops {@link DevModeHandler} if there is no anymore initialized servlet
 * context instances.
 *
 * @author Vaadin Ltd
 *
 */
class StopDevMode implements ServletContextListener, Serializable {

    private static final AtomicInteger SERVLET_CONTEXTS = new AtomicInteger();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SERVLET_CONTEXTS.incrementAndGet();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (SERVLET_CONTEXTS.decrementAndGet() == 0) {
            DevModeHandler handler = DevModeHandler.getDevModeHandler();
            if (handler != null) {
                handler.stop();
                System.setProperty(VAADIN_PREFIX
                        + SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT, null);
            }
        }
    }

}
