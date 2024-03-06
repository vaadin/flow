/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;

/**
 * Passes Spring application properties to the Vaadin application configuration.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public class SpringApplicationConfigurationFactory
        extends DefaultApplicationConfigurationFactory {

    @Override
    protected ApplicationConfigurationImpl doCreate(VaadinContext context,
            FallbackChunk chunk, Map<String, String> properties) {
        ApplicationContext appContext = SpringLookupInitializer
                .getApplicationContext(context);
        Environment env = appContext.getBean(Environment.class);
        // Collect any vaadin.XZY properties from application.properties
        SpringServlet.PROPERTY_NAMES.stream()
                .filter(name -> env.getProperty("vaadin." + name) != null)
                .forEach(name -> properties.put(name,
                        env.getProperty("vaadin." + name)));
        return super.doCreate(context, chunk, properties);
    }
}
