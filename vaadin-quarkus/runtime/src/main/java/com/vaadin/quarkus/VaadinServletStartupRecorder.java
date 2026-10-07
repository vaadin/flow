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
package com.vaadin.quarkus;

import jakarta.servlet.ServletException;

import java.util.List;

import io.quarkus.runtime.annotations.Recorder;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.core.ManagedServlets;

/**
 * Initializes the Vaadin servlets of a native image at RUNTIME_INIT.
 * <p>
 * Quarkus initializes load-on-startup servlets while it deploys the servlet
 * container at STATIC_INIT, which runs while a native image is built. In a
 * native image, the Vaadin servlets are therefore registered without
 * load-on-startup and initialized here instead, so that the Vaadin service and
 * its deployment configuration are created from the runtime configuration.
 * <p>
 * This class is meant for internal use only.
 */
@Recorder
public class VaadinServletStartupRecorder {

    /**
     * Initializes the given servlets, in the given order.
     *
     * @param deploymentManager
     *            the manager of the servlet deployment
     * @param servletNames
     *            the names of the servlets to initialize
     */
    public void initServlets(DeploymentManager deploymentManager,
            List<String> servletNames) {
        ManagedServlets servlets = deploymentManager.getDeployment()
                .getServlets();
        for (String servletName : servletNames) {
            try {
                servlets.getManagedServlet(servletName).forceInit();
            } catch (ServletException e) {
                throw new IllegalStateException(
                        "Unable to initialize the Vaadin servlet "
                                + servletName,
                        e);
            }
        }
    }
}
