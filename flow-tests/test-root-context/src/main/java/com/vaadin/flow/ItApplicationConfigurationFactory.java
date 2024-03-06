/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import java.util.Map;

import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;

@Component(service = ApplicationConfigurationFactory.class, property = org.osgi.framework.Constants.SERVICE_RANKING
        + ":Integer=" + Integer.MAX_VALUE)
public class ItApplicationConfigurationFactory
        extends DefaultApplicationConfigurationFactory {

    @Override
    protected ApplicationConfigurationImpl doCreate(VaadinContext context,
            FallbackChunk chunk, Map<String, String> properties) {
        properties.put(Constants.ALLOW_APPSHELL_ANNOTATIONS,
                Boolean.TRUE.toString());
        return super.doCreate(context, chunk, properties);
    }
}
