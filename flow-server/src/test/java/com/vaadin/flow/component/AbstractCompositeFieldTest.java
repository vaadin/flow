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

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.AbstractSinglePropertyFieldTest.StringField;
import com.vaadin.flow.component.ComponentTest.TestDiv;

public class AbstractCompositeFieldTest {

    private static class ReverseCaseField extends
            AbstractCompositeField<AbstractSinglePropertyFieldTest.StringField, ReverseCaseField, String> {
        public ReverseCaseField() {
            super("");

            getContent().addValueChangeListener(event -> {
                setModelValue(reverseCase(event.getValue()),
                        event.isFromClient());
            });
        }

        @Override
        protected void setPresentationValue(String newPresentationValue) {
            getContent().setValue(reverseCase(newPresentationValue));
        }

        @Override
        protected boolean valueEquals(String value1, String value2) {
            if (value1 != null && value2 != null) {
                return value1.trim().equals(value2.trim());
            } else {
                return value1 == value2;
            }
        }

        private static String reverseCase(String input) {
            int[] reversedCodePoints = input.codePoints().map(c -> {
                if (Character.isUpperCase(c)) {
                    return Character.toLowerCase(c);
                } else if (Character.isLowerCase(c)) {
                    return Character.toUpperCase(c);
                } else {
                    return c;
                }
            }).toArray();

            return new String(reversedCodePoints, 0, reversedCodePoints.length);
        }
    }

    @Test
    public void reverseCaseField() {
        ReverseCaseField outerField = new ReverseCaseField();
        StringField innerField = outerField.getContent();

        outerField.setValue("Hello");
        Assert.assertEquals("hELLO", innerField.getValue());

        innerField.setValue("wORLD");
        Assert.assertEquals("World", outerField.getValue());
    }

    @Test
    public void emptyValueEquals() {
        ReverseCaseField field = new ReverseCaseField();

        Assert.assertTrue(field.isEmpty());

        field.setValue("a");
        Assert.assertFalse(field.isEmpty());

        field.setValue(" ");
        Assert.assertTrue(field.isEmpty());
    }

    private static class MultipleFieldsField extends
            AbstractCompositeField<TestDiv, MultipleFieldsField, String> {
        private StringField start = new StringField();
        private StringField rest = new StringField();

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
    public void multipleFieldsField() {
        MultipleFieldsField field = new MultipleFieldsField();

        field.setValue("Hello Cool World");
        Assert.assertEquals("Hello", field.start.getValue());
        Assert.assertEquals("Cool World", field.rest.getValue());

        field.rest.setValue("");
        Assert.assertEquals("Hello", field.getValue());

        field.rest.setValue("Vaadin");
        Assert.assertEquals("Hello Vaadin", field.getValue());
    }

    @Test
    public void serializable() {
        ReverseCaseField field = new ReverseCaseField();
        field.addValueChangeListener(ignore -> {
        });
        field.setValue("foo");

        ReverseCaseField anotherField = SerializationUtils.roundtrip(field);
        Assert.assertEquals("foo", anotherField.getValue());
    }

}
