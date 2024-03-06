/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import javax.servlet.ServletContext;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.spring.SpringLookupInitializer.SpringApplicationContextInit;

public class SpringApplicationConfigurationFactoryTest {

    private WebApplicationContext webAppContext = Mockito
            .mock(WebApplicationContext.class);

    private ServletContext servletContext = Mockito.mock(ServletContext.class);

    private VaadinServletContext context = new VaadinServletContext(
            servletContext);

    private Environment env = Mockito.mock(Environment.class);

    private SpringApplicationConfigurationFactory factory = new SpringApplicationConfigurationFactory();

    private Map<String, Object> map = new HashMap<>();

    @Before
    public void setUp() {
        Mockito.doAnswer(invocation -> {
            map.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(servletContext).setAttribute(Mockito.anyString(),
                Mockito.any());

        Mockito.doAnswer(invocation -> {
            return map.get(invocation.getArgument(0));
        }).when(servletContext).getAttribute(Mockito.anyString());

        Mockito.when(servletContext.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(webAppContext);

        Mockito.when(webAppContext.getBean(Environment.class)).thenReturn(env);
    }

    @Test
    public void doCreate_vaadinApplicationConfigurationHasSpringPropertiesPrefixedByVaadin() {
        String prefix = "foo_";
        SpringServlet.PROPERTY_NAMES.stream()
                .forEach(name -> Mockito.when(env.getProperty("vaadin." + name))
                        .thenReturn(prefix + name));
        Map<String, String> props = new HashMap<>();
        ApplicationConfiguration config = factory.doCreate(context, null,
                props);

        for (String prop : SpringServlet.PROPERTY_NAMES) {
            Assert.assertEquals(
                    "'" + prop + "' property is not available via "
                            + ApplicationConfiguration.class,
                    prefix + prop, config.getStringProperty(prop, null));
            Assert.assertEquals("'" + prop
                    + "' property is not set in the properties map passed to the "
                    + ApplicationConfiguration.class.getSimpleName() + " CTOR",
                    prefix + prop, props.get(prop));
        }

        Assert.assertEquals(SpringServlet.PROPERTY_NAMES.size(), props.size());
    }

    @Test
    public void doCreate__servletContextIsNotYetAvailableViaSrpingUtils_vaadinApplicationConfigurationHasSpringPropertiesPrefixedByVaadin() {
        Mockito.when(webAppContext.getServletContext())
                .thenReturn(servletContext);
        Mockito.when(servletContext.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(null);

        new SpringApplicationContextInit().setApplicationContext(webAppContext);

        doCreate_vaadinApplicationConfigurationHasSpringPropertiesPrefixedByVaadin();
    }
}
