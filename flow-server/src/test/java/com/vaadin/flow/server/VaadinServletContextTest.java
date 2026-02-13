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
package com.vaadin.flow.server;

import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for VaadinServletContext attribute storage and property delegation.
 *
 * @since 2.0.0
 */
class VaadinServletContextTest {

    private static String testAttributeProvider() {
        return "RELAX_THIS_IS_A_TEST";
    }

    private VaadinServletContext context;

    private final Map<String, Object> attributeMap = new HashMap<>();
    private Map<String, String> properties;

    @BeforeEach
    public void setup() {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getAttribute(Mockito.anyString()))
                .then(invocationOnMock -> attributeMap
                        .get(invocationOnMock.getArguments()[0].toString()));
        Mockito.doAnswer(invocationOnMock -> {
            attributeMap.remove(invocationOnMock.getArguments()[0].toString());
            return null;
        }).when(servletContext).removeAttribute(Mockito.anyString());

        Mockito.doAnswer(invocationOnMock -> attributeMap.put(
                invocationOnMock.getArguments()[0].toString(),
                invocationOnMock.getArguments()[1])).when(servletContext)
                .setAttribute(Mockito.anyString(), Mockito.any());

        properties = new HashMap<>();
        properties.put(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE,
                "true");
        properties.put(InitParameters.FRONTEND_HOTDEPLOY, "false");

        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.enumeration(properties.keySet()));
        Mockito.when(servletContext.getInitParameter(Mockito.anyString())).then(
                invocation -> properties.get(invocation.getArguments()[0]));
        context = new VaadinServletContext(servletContext);
    }

    @Test
    public void getAttributeWithProvider() {
        assertNull(context.getAttribute(String.class));

        String value = context.getAttribute(String.class,
                VaadinServletContextTest::testAttributeProvider);
        assertEquals(testAttributeProvider(), value);

        assertEquals(testAttributeProvider(),
                context.getAttribute(String.class),
                "Value from provider should be persisted");
    }

    @Test
    public void setNullAttributeNotAllowed() {
        assertThrows(AssertionError.class, () -> {
            context.setAttribute(null);
        });
    }

    @Test
    public void getMissingAttributeWithoutProvider() {
        String value = context.getAttribute(String.class);
        assertNull(value);
    }

    @Test
    public void setAndGetAttribute() {
        String value = testAttributeProvider();
        context.setAttribute(value);
        String result = context.getAttribute(String.class);
        assertEquals(value, result);
        // overwrite
        String newValue = "this is a new value";
        context.setAttribute(newValue);
        result = context.getAttribute(String.class);
        assertEquals(newValue, result);
        // now the provider should not be called, so value should be still there
        result = context.getAttribute(String.class, () -> {
            throw new AssertionError("Should not be called");
        });
        assertEquals(newValue, result);
    }

    @Test
    public void setValueBasedOnSuperType_implicitClass_notFound() {
        String value = testAttributeProvider();
        context.setAttribute(value);

        CharSequence retrieved = context.getAttribute(CharSequence.class);
        assertNull(retrieved,
                "Value set base on its own type should not be found based on a super type");
    }

    @Test
    public void setValueBasedOnSuperType_explicitClass_found() {
        String value = testAttributeProvider();
        context.setAttribute(CharSequence.class, value);

        CharSequence retrieved = context.getAttribute(CharSequence.class);
        assertSame(value, retrieved,
                "Value should be found based on the type used when setting");
    }

    @Test
    public void removeValue_removeMethod_valueIsRemoved() {
        context.setAttribute(testAttributeProvider());
        context.removeAttribute(String.class);

        assertNull(context.getAttribute(String.class),
                "Value should be removed");
    }

    @Test
    public void removeValue_setWithClass_valueIsRemoved() {
        context.setAttribute(testAttributeProvider());
        context.setAttribute(String.class, null);

        assertNull(context.getAttribute(String.class),
                "Value should be removed");
    }

    @Test
    public void getPropertyNames_returnsExpectedProperties() {
        List<String> list = Collections
                .list(context.getContextParameterNames());
        assertEquals(properties.size(), list.size(),
                "Context should return only keys defined in ServletContext");
        for (String key : properties.keySet()) {
            assertEquals(properties.get(key), context.getContextParameter(key),
                    String.format(
                            "Value should be same from context for key '%s'",
                            key));
        }
    }
}
