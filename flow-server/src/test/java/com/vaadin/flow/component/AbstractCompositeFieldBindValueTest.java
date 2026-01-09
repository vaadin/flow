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
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.AbstractSinglePropertyFieldTest.StringField;
import com.vaadin.flow.component.ComponentTest.TestDiv;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;
import com.vaadin.signals.WritableSignal;

public class AbstractCompositeFieldBindValueTest extends SignalsUnitTest {

    private static class MultipleFieldsField extends
            AbstractCompositeField<TestDiv, MultipleFieldsField, String> {
        private final StringField start = new StringField();
        private final StringField rest = new StringField();

        public MultipleFieldsField() {
            super(null);

            getContent().getElement().appendChild(start.getElement(),
                    rest.getElement());

            start.addValueChangeListener(
                    event -> updateValue(event.isFromClient()));
            rest.addValueChangeListener(
                    event -> updateValue(event.isFromClient()));
        }

        private void updateValue(boolean fromClient) {
            String value = start.getValue();

            String restValue = rest.getValue();
            if (!restValue.isEmpty()) {
                value += " " + restValue;
            }

            setModelValue(value, fromClient);
        }

        @Override
        protected void setPresentationValue(String newPresentationValue) {
            String[] parts = newPresentationValue.split(" ", 2);
            start.setValue(parts[0]);

            if (parts.length > 1) {
                rest.setValue(parts[1]);
            } else {
                rest.setValue("");
            }
        }
    }

    @Test
    public void multipleFieldsField_bindValue_detached_setModalValueDoesNotUpdateSignal() {
        MultipleFieldsField field = new MultipleFieldsField();

        WritableSignal<String> signal = new ValueSignal<>("Hello Cool World");
        field.bindValue(signal);
        // not attached yet, so presentation value not used from the signal
        Assert.assertEquals("", field.start.getValue());
        Assert.assertEquals("", field.rest.getValue());

        // test that setModelValue updates the bound signal even when detached
        field.start.setValue("Hey");
        field.rest.setValue("You");
        Assert.assertEquals("Hey You", field.getValue());
        Assert.assertEquals("Hello Cool World", signal.peek());
    }

    @Test
    public void multipleFieldsField_bindValue_attached() {
        MultipleFieldsField field = new MultipleFieldsField();
        UI.getCurrent().add(field);

        WritableSignal<String> signal = new ValueSignal<>("Hello Cool World");
        field.bindValue(signal);
        Assert.assertEquals("Hello", field.start.getValue());
        Assert.assertEquals("Cool World", field.rest.getValue());

        // test that setValue fails when bound
        Assert.assertThrows(BindingActiveException.class,
                () -> field.setValue(""));

        // setValue for CompositeField's components is allowed since their value
        // change listeners update the value by internal setModelValue method
        field.rest.setValue("");
        Assert.assertEquals("Hello", field.getValue());
        Assert.assertEquals("Hello", signal.peek());

        field.rest.setValue("Vaadin");
        Assert.assertEquals("Hello Vaadin", field.getValue());
        Assert.assertEquals("Hello Vaadin", signal.peek());

        // remove binding. Value should stay the same.
        field.bindValue(null);
        Assert.assertEquals("Hello Vaadin", field.getValue());
        Assert.assertEquals("Hello Vaadin", signal.peek());

        // test that setValue works after unbinding
        field.setValue("Hey You");
        Assert.assertEquals("Hey You", field.getValue());
        Assert.assertEquals("Hello Vaadin", signal.peek());
    }
}
