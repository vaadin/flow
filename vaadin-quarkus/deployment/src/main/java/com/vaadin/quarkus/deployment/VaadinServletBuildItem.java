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
package com.vaadin.quarkus.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A Vaadin servlet of a native image, which is initialized at RUNTIME_INIT
 * instead of by the servlet container at STATIC_INIT.
 */
public final class VaadinServletBuildItem extends MultiBuildItem {

    private final String servletName;
    private final int loadOnStartup;

    /**
     * Creates a new build item.
     *
     * @param servletName
     *            the name the servlet is registered with
     * @param loadOnStartup
     *            the order in which the servlet is initialized, lower first
     */
    public VaadinServletBuildItem(String servletName, int loadOnStartup) {
        this.servletName = servletName;
        this.loadOnStartup = loadOnStartup;
    }

    /**
     * Gets the name the servlet is registered with.
     *
     * @return the servlet name
     */
    public String getServletName() {
        return servletName;
    }

    /**
     * Gets the order in which the servlet is initialized, lower first.
     *
     * @return the load-on-startup value
     */
    public int getLoadOnStartup() {
        return loadOnStartup;
    }
}
