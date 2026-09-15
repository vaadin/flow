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
package com.vaadin.quarkus.graal;

import java.util.Enumeration;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.atmosphere.cpr.ApplicationConfig;

/**
 * A {@link ServletConfig} wrapper that forces the usage of
 * {@link DelayedInitBroadcaster} to prevent executors to be started during
 * static init in a native build.
 */
public class AtmosphereServletConfig implements ServletConfig {

    private final ServletConfig delegate;

    public AtmosphereServletConfig(ServletConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getServletName() {
        return delegate.getServletName();
    }

    @Override
    public ServletContext getServletContext() {
        return delegate.getServletContext();
    }

    @Override
    public String getInitParameter(String name) {
        if (ApplicationConfig.BROADCASTER_CLASS.equals(name)) {
            return DelayedInitBroadcaster.class.getName();
        }
        return delegate.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return delegate.getInitParameterNames();
    }
}
