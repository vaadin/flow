/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;

/**
 * An abstract base class for DeploymentConfiguration implementations. This
 * class provides default implementation for common config properties.
 *
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractDeploymentConfiguration
        implements DeploymentConfiguration {

    @Override
    public String getUIClassName() {
        return getStringProperty(InitParameters.UI_PARAMETER,
                UI.class.getName());
    }

    @Override
    public String getClassLoaderName() {
        return getStringProperty("ClassLoader", null);
    }

}
