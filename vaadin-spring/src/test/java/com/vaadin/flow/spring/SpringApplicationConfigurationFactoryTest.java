/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import jakarta.servlet.ServletContext;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.spring.SpringLookupInitializer.SpringApplicationContextInit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringApplicationConfigurationFactoryTest {

    private WebApplicationContext webAppContext = Mockito
            .mock(WebApplicationContext.class);

    private ServletContext servletContext = Mockito.mock(ServletContext.class);

    private VaadinServletContext context = new VaadinServletContext(
            servletContext);

    private Environment env = Mockito.mock(Environment.class);

    private SpringApplicationConfigurationFactory factory = new SpringApplicationConfigurationFactory();

    private Map<String, Object> map = new HashMap<>();

    @BeforeEach
    void setUp() {
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
    void doCreate_vaadinApplicationConfigurationHasSpringPropertiesPrefixedByVaadin() {
        String prefix = "foo_";
        SpringServlet.PROPERTY_NAMES.stream()
                .forEach(name -> Mockito.when(env.getProperty("vaadin." + name))
                        .thenReturn(prefix + name));
        Map<String, String> props = new HashMap<>();
        ApplicationConfiguration config = factory.doCreate(context, props);

        for (String prop : SpringServlet.PROPERTY_NAMES) {
            assertEquals(prefix + prop, config.getStringProperty(prop, null),
                    "'" + prop + "' property is not available via "
                            + ApplicationConfiguration.class);
            assertEquals(prefix + prop, props.get(prop), "'" + prop
                    + "' property is not set in the properties map passed to the "
                    + ApplicationConfiguration.class.getSimpleName() + " CTOR");
        }

        assertEquals(SpringServlet.PROPERTY_NAMES.size(), props.size());
    }

    @Test
    void doCreate__servletContextIsNotYetAvailableViaSrpingUtils_vaadinApplicationConfigurationHasSpringPropertiesPrefixedByVaadin() {
        Mockito.when(webAppContext.getServletContext())
                .thenReturn(servletContext);
        Mockito.when(servletContext.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(null);

        new SpringApplicationContextInit().setApplicationContext(webAppContext);

        doCreate_vaadinApplicationConfigurationHasSpringPropertiesPrefixedByVaadin();
    }
}
