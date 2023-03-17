/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion;

import javax.servlet.ServletContext;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.mockito.Mockito;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

@Component
public class ServletContextTestSetup implements ServletContextAware {

    @Override
    public void setServletContext(ServletContext servletContext) {
        Lookup lookup = Mockito.mock(Lookup.class);
        servletContext.setAttribute(Lookup.class.getName(), lookup);
        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        servletContext.setAttribute(ApplicationConfiguration.class.getName(),
                applicationConfiguration);

    }

}
