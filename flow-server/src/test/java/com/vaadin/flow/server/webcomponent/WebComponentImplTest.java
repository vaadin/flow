/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.server.webcomponent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentBinding;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebComponentImplTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private WebComponentImpl<Component> webComponent;

    @Before
    public void init() {
        webComponent = new WebComponentImpl<>(
                mock(WebComponentBinding.class),
                new Element("tag"));
    }

    @Test
    public void fireEvent_throwsWhenNameIsNull() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("eventName");
        webComponent.fireEvent(null);
    }

    @Test
    public void fireEvent_doesNotThrowOnNullObjectData() {
        webComponent.fireEvent("name", null);
    }

    @Test
    public void fireEvent_throwsWhenOptionsIsNull() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("options");
        webComponent.fireEvent("name", null, null);
    }

    @Test
    public void setProperty_throwsOnNullPropertyConfiguration() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("propertyConfiguration");
        webComponent.setProperty(null, "value");
    }

    @Test
    public void setProperty_throwsIfConfigurationIsNotAValidImplementation() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("must be an implementation of");

        webComponent.setProperty(new CustomConfiguration(), "value");
    }

    @Test
    public void setProperty_throwsOnUnknownProperty() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("WebComponent does not have a property identified");

        WebComponentBinding<Component> binding =
                mock(WebComponentBinding.class);
        when(binding.hasProperty(anyString())).thenReturn(false);

        WebComponentImpl<Component> webComponent =
                new WebComponentImpl<>(binding,
                new Element("tag"));

        PropertyConfigurationImpl<Component, String> configuration =
                new PropertyConfigurationImpl<>(Component.class, "property",
                        String.class, "value");

        webComponent.setProperty(configuration, "newValue");
    }

    @Test
    public void setProperty_throwsWhenGivenWrongPropertyTypeAsParameter() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Property 'property' of type " +
                "'java.lang.Integer' cannot be assigned value of type " +
                "'java.lang.String'!");

        WebComponentBinding<Component> binding =
                mock(WebComponentBinding.class);
        when(binding.hasProperty(anyString())).thenReturn(true);
        when(binding.getPropertyType(anyString())).thenReturn((Class)Integer.class);

        WebComponentImpl<Component> webComponent =
                new WebComponentImpl<>(binding, new Element("tag"));

        PropertyConfigurationImpl<Component, String> configuration =
                new PropertyConfigurationImpl<>(Component.class, "property",
                        String.class, "value");

        webComponent.setProperty(configuration, "newValue");
    }

    private static class CustomConfiguration implements PropertyConfiguration<Component, String> {

        @Override
        public PropertyConfiguration<Component, String> onChange(SerializableBiConsumer<Component, String> onChangeHandler) {
            return null;
        }

        @Override
        public PropertyConfiguration<Component, String> readOnly() {
            return null;
        }
    }
}
