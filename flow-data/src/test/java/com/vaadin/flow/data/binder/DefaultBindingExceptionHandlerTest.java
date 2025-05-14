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
package com.vaadin.flow.data.binder;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class DefaultBindingExceptionHandlerTest {

    private DefaultBindingExceptionHandler handler = new DefaultBindingExceptionHandler();

    private abstract class TestComponent implements HasElement, HasValue {

    }

    private TestComponent component = Mockito.mock(TestComponent.class);

    private Element element = ElementFactory.createAnchor();

    @Before
    public void setUp() {
        Mockito.when(component.getElement()).thenReturn(element);
    }

    @Test
    public void handleException_elementHasId_messageContainsIdReference() {
        element.setProperty("id", "foo");
        Optional<BindingException> result = handler.handleException(component,
                new Exception());

        String message = result.get().getMessage();
        Assert.assertEquals(
                "An exception has been thrown inside binding logic for the field element [id='foo']",
                message);
    }

    @Test
    public void handleException_elementHasLabel_messageContainsLabelReference() {
        element.setProperty("label", "foo");
        Optional<BindingException> result = handler.handleException(component,
                new Exception());

        String message = result.get().getMessage();
        Assert.assertEquals(
                "An exception has been thrown inside binding logic for the field element [label='foo']",
                message);
    }

    @Test
    public void handleException_elementHasNoLabelAndId_devMode_messageContainsPropertiesReference() {
        UI ui = mockUI(false);
        ui.getElement().appendChild(element);

        element.setProperty("foo", "bar");
        element.setAttribute("baz", "foo-bar");
        Optional<BindingException> result = handler.handleException(component,
                new Exception());

        String message = result.get().getMessage();
        Assert.assertEquals(
                "An exception has been thrown inside binding logic for the field element [baz='foo-bar', foo='bar']",
                message);
    }

    @Test
    public void handleException_elementHasNoLabelAndId_prodcutionMode_returnsEmpty() {
        UI ui = mockUI(true);
        ui.getElement().appendChild(element);

        element.setProperty("foo", "bar");
        element.setAttribute("baz", "foo-bar");
        Optional<BindingException> result = handler.handleException(component,
                new Exception());

        Assert.assertFalse(result.isPresent());
    }

    private UI mockUI(boolean productionMode) {
        UI ui = new UI();
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        ui.getInternals().setSession(session);

        Mockito.when(session.getService()).thenReturn(service);

        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);

        ApplicationConfiguration configuration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(context.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(configuration);
        Mockito.when(configuration.isProductionMode())
                .thenReturn(productionMode);

        return ui;
    }

}
