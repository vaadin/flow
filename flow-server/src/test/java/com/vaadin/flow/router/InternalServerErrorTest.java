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
package com.vaadin.flow.router;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class InternalServerErrorTest {

    private BeforeEnterEvent event = Mockito.mock(BeforeEnterEvent.class);

    private VaadinService service = Mockito.mock(VaadinService.class);

    private DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);

    @BeforeEach
    public void setUp() {
        VaadinService.setCurrent(service);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        Location location = new Location("bar");
        Mockito.when(event.getLocation()).thenReturn(location);
    }

    @AfterEach
    public void tearDown() {
        VaadinService.setCurrent(null);
    }

    @Test
    public void productionMode_noWarningAndStacktrace() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        InternalServerError testInstance = new InternalServerError();

        testInstance.setErrorParameter(event, new ErrorParameter<>(
                Exception.class, new NullPointerException("foo")));

        assertEquals(1, testInstance.getElement().getChildCount(),
                "Only a text node with exception message should be shown");
    }

    @Test
    public void nonProductionMode_noLogBinding_showWaringAndStacktrace() {
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        InternalServerError testInstance = new InternalServerError() {

            @Override
            protected boolean hasLogBinding() {
                return false;
            }
        };

        testInstance.setErrorParameter(event, new ErrorParameter<>(
                Exception.class, new NullPointerException("foo")));

        assertEquals(3, testInstance.getElement().getChildCount(),
                "3 elements should be shown: exception text, warning about log binding absence and exception stacktrace");

        Element warning = testInstance.getElement().getChild(1);
        assertEquals("div", warning.getTag());
        assertTrue(warning.getText().contains("SLF4J"));

        Element stacktrace = testInstance.getElement().getChild(2);
        assertEquals("pre", stacktrace.getTag());
        assertTrue(stacktrace.getText()
                .contains(NullPointerException.class.getName()));
        assertTrue(stacktrace.getText().contains("foo"));
    }

    @Test
    public void nonProductionMode_hasLogBinding_showStacktraceAndNoWarning() {
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        InternalServerError testInstance = new InternalServerError() {

            @Override
            protected boolean hasLogBinding() {
                return true;
            }
        };

        testInstance.setErrorParameter(event, new ErrorParameter<>(
                Exception.class, new NullPointerException("foo")));

        assertEquals(2, testInstance.getElement().getChildCount(),
                "2 elements should be shown: exception text and exception stacktrace");

        Element stacktrace = testInstance.getElement().getChild(1);
        assertEquals("pre", stacktrace.getTag());
        assertTrue(stacktrace.getText()
                .contains(NullPointerException.class.getName()));
        assertTrue(stacktrace.getText().contains("foo"));
    }
}
