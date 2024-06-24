/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.osgi;

import java.io.Serializable;

import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.osgi.support.OsgiVaadinStaticResource;

/**
 * Flow component renderer resource registration.
 *
 * @author Vaadin Ltd
 * @since 1.2
 */
@Component(immediate = true, service = OsgiVaadinStaticResource.class)
public class FlowComponentRendererResource
        implements OsgiVaadinStaticResource, Serializable {

    @Override
    public String getPath() {
        return "/META-INF/resources/frontend/flow-component-renderer.html";
    }

    @Override
    public String getAlias() {
        return "/frontend/flow-component-renderer.html";
    }

}
