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
package com.vaadin.flow.router;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;

@NotThreadSafe
public class InternalServerErrorTest {

    private BeforeEnterEvent event = Mockito.mock(BeforeEnterEvent.class);

    private VaadinService service = Mockito.mock(VaadinService.class);

    private DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);

    @Before
    public void setUp() {
        VaadinService.setCurrent(service);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        Location location = new Location("bar");
        Mockito.when(event.getLocation()).thenReturn(location);
    }

    @After
    public void tearDown() {
        VaadinService.setCurrent(null);
    }

    @Test
    public void productionMode_noWarningAndStacktrace() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        InternalServerError testInstance = new InternalServerError();

        testInstance.setErrorParameter(event, new ErrorParameter<>(
                Exception.class, new NullPointerException("foo")));

        Assert.assertEquals(
                "Only a text node with exception message should be shown", 1,
                testInstance.getElement().getChildCount());
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

        Assert.assertEquals(
                "3 elements should be shown: exception text, warning about log binding absence and exception stacktrace",
                3, testInstance.getElement().getChildCount());

        Element warning = testInstance.getElement().getChild(1);
        Assert.assertEquals("div", warning.getTag());
        Assert.assertTrue(warning.getText().contains("SLF4J"));

        Element stacktrace = testInstance.getElement().getChild(2);
        Assert.assertEquals("pre", stacktrace.getTag());
        Assert.assertTrue(stacktrace.getText()
                .contains(NullPointerException.class.getName()));
        Assert.assertTrue(stacktrace.getText().contains("foo"));
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

        Assert.assertEquals(
                "2 elements should be shown: exception text and exception stacktrace",
                2, testInstance.getElement().getChildCount());

        Element stacktrace = testInstance.getElement().getChild(1);
        Assert.assertEquals("pre", stacktrace.getTag());
        Assert.assertTrue(stacktrace.getText()
                .contains(NullPointerException.class.getName()));
        Assert.assertTrue(stacktrace.getText().contains("foo"));
    }
}
