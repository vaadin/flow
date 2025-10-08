/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test VaadinServletConfig property handling and function with VaadinContext.
 */
public class VaadinServletConfigTest {

    private VaadinServletConfig config;

    private ServletContext servletContext;
    private final Map<String, Object> attributeMap = new HashMap<>();
    private Map<String, String> properties;

    @Before
    public void setup() {
        ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
        servletContext = Mockito.mock(ServletContext.class);

        Mockito.when(servletConfig.getServletContext())
                .thenReturn(servletContext);

        Mockito.when(servletContext.getAttribute(Mockito.anyString()))
                .then(invocationOnMock -> attributeMap
                        .get(invocationOnMock.getArguments()[0].toString()));
        Mockito.doAnswer(invocationOnMock -> attributeMap.put(
                invocationOnMock.getArguments()[0].toString(),
                invocationOnMock.getArguments()[1])).when(servletContext)
                .setAttribute(Mockito.anyString(), Mockito.any());

        properties = new HashMap<>();
        properties.put(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE,
                "true");
        properties.put(InitParameters.FRONTEND_HOTDEPLOY, "false");

        Mockito.when(servletConfig.getInitParameterNames())
                .thenReturn(Collections.enumeration(properties.keySet()));
        Mockito.when(servletConfig.getInitParameter(Mockito.anyString())).then(
                invocation -> properties.get(invocation.getArguments()[0]));
        config = new VaadinServletConfig(servletConfig);
    }

    @Test
    public void getPropertyNames_returnsExpectedProperties() {
        List<String> list = Collections.list(config.getConfigParameterNames());
        Assert.assertEquals(
                "Context should return only keys defined in ServletContext",
                properties.size(), list.size());
        for (String key : properties.keySet()) {
            Assert.assertEquals(String.format(
                    "Value should be same from context for key '%s'", key),
                    properties.get(key), config.getConfigParameter(key));
        }
    }

    @Test
    public void vaadinContextThroughConfig_setAndGetAttribute() {
        String value = "my-attribute";
        config.getVaadinContext().setAttribute(value);
        String result = config.getVaadinContext().getAttribute(String.class);
        Assert.assertEquals(value, result);
        // overwrite
        String newValue = "this is a new value";
        config.getVaadinContext().setAttribute(newValue);
        result = config.getVaadinContext().getAttribute(String.class);
        Assert.assertEquals(newValue, result);
        // now the provider should not be called, so value should be still there
        result = config.getVaadinContext().getAttribute(String.class, () -> {
            throw new AssertionError("Should not be called");
        });
        Assert.assertEquals(newValue, result);
    }
}
