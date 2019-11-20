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

import java.io.Serializable;
import java.util.Enumeration;

/**
 * Configuration in which {@link VaadinService} is running.
 * This is a wrapper for Config objects for instance <code>ServletConfig</code>
 * and <code>PortletConfig</code>.
 *
 * @since
 */
public interface VaadinConfig extends Serializable {

    /**
     * Get the VaadinContext for this configuration.
     *
     * @return VaadinContext object for this VaadinConfiguration
     */
    VaadinContext getVaadinContext();

    /**
     * Returns the names of the initialization parameters as an
     * <code>Enumeration</code>, or an empty <code>Enumeration</code> if there
     * are o initialization parameters.
     *
     * @return initialization parameters as a <code>Enumeration</code>
     */
    Enumeration<String> getConfigParameterNames();

    /**
     * Returns the value for the requested parameter, or <code>null</code> if
     * the parameter does not exist.
     *
     * @param name
     *         name of the parameter whose value is requested
     * @return parameter value as <code>String</code> or <code>null</code> for
     * no parameter
     */
    String getConfigParameter(String name);
}
