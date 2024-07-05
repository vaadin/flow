/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.webcomponent;

import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;

/**
 * Data collector component for collecting web components in an OSGi
 * environment.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class OSGiWebComponentConfigurationRegistry
        extends WebComponentConfigurationRegistry {

    @Override
    public boolean setConfigurations(
            Set<WebComponentConfiguration<? extends Component>> configurations) {

        updateRegistry(configurations);
        return true;
    }
}
