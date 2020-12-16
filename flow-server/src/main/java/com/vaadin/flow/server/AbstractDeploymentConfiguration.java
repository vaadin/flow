/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.Map;

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
public abstract class AbstractDeploymentConfiguration extends
        AbstractPropertyConfiguration implements DeploymentConfiguration {

    /**
     * Creates a new configuration based on {@code properties}.
     * 
     * @param properties
     *            configuration properties
     */
    protected AbstractDeploymentConfiguration(Map<String, String> properties) {
        super(properties);
    }

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
