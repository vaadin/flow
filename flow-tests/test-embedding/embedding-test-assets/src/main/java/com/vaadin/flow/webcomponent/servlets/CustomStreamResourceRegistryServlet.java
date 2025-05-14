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
package com.vaadin.flow.webcomponent.servlets;

import jakarta.servlet.annotation.WebServlet;

import java.net.URI;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

@WebServlet(urlPatterns = { "/vaadin-stream-resources/*",
        "/vaadin-stream-resources-absolute/*" }, asyncSupported = true)
public class CustomStreamResourceRegistryServlet extends VaadinServlet {

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinServletService service = new VaadinServletService(this,
                deploymentConfiguration) {

            @Override
            protected VaadinSession createVaadinSession(VaadinRequest request) {
                return new VaadinSession(this) {
                    @Override
                    protected StreamResourceRegistry createStreamResourceRegistry() {
                        return new StreamResourceRegistry(this) {
                            @Override
                            public URI getTargetURI(
                                    AbstractStreamResource resource) {
                                URI targetURI = super.getTargetURI(resource);
                                if (resource.getName()
                                        .contains("absoluteURL")) {
                                    String baseURL = getCurrentServletRequest()
                                            .getRequestURL().toString()
                                            .replaceFirst(
                                                    "(/vaadin-stream-resources)/.*",
                                                    "$1-absolute/");
                                    if (resource.getName()
                                            .contains("schemaless")) {
                                        baseURL = baseURL.replaceFirst(
                                                "^https?://", "//");
                                    }
                                    return URI.create(baseURL)
                                            .resolve(targetURI);
                                }
                                return targetURI;
                            }
                        };
                    }
                };
            }
        };
        service.init();
        return service;
    }
}
