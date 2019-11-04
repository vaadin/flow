/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import javax.servlet.ServletConfig;
import java.util.Enumeration;
import java.util.Objects;

/**
 * {@link VaadinConfig} implementation for Servlets.
 *
 * @since
 */
public class VaadinServletConfig implements VaadinConfig {

    private transient ServletConfig config;

    /**
     * Vaadin servlet configuration wrapper constructor.
     *
     * @param config
     *         servlet configuration object, not <code>null</code>
     */
    public VaadinServletConfig(ServletConfig config) {
        Objects.requireNonNull(config, "VaadinServletConfig requires the ServletConfig object");
        this.config = config;
    }

    /**
     * Ensures there is a valid instance of {@link ServletConfig}.
     */
    private void ensureServletConfig() {
        if (config == null && VaadinService
                .getCurrent() instanceof VaadinServletService) {
            config = ((VaadinServletService) VaadinService.getCurrent())
                    .getServlet().getServletConfig();
        } else if (config == null) {
            throw new IllegalStateException(
                    "The underlying ServletContext of VaadinServletContext is null and there is no VaadinServletService to obtain it from.");
        }
    }

    @Override
    public VaadinContext getVaadinContext() {
        ensureServletConfig();
        return new VaadinServletContext(config.getServletContext());
    }

    @Override
    public Enumeration<String> getConfigParameterNames() {
        ensureServletConfig();
        return config.getInitParameterNames();
    }

    @Override
    public String getConfigParameter(String name) {
        ensureServletConfig();
        return config.getInitParameter(name);
    }
}
