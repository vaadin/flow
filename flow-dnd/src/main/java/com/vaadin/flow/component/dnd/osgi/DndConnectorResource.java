/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component.dnd.osgi;

import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.osgi.support.OsgiVaadinStaticResource;

/**
 * Connector resource registration.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
@Component(immediate = true, service = OsgiVaadinStaticResource.class)
public class DndConnectorResource implements OsgiVaadinStaticResource {

    @Override
    public String getPath() {
        return "/META-INF/resources/frontend/dndConnector.js";
    }

    @Override
    public String getAlias() {
        return "/frontend/dndConnector.js";
    }

}
