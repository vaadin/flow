/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiPredicate;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.tests.PublicApiAnalyzer;

public class AbstractFieldTest {
    // This isn't a test in itself, but it shows that no more than one method
    // needs to be overridden
    private static class SimpleAbstractField<T>
            extends AbstractField<SimpleAbstractField<T>, T> {

        public SimpleAbstractField() {
            super(null);
        }

        @Override
        protected void setPresentationValue(T value) {
        }
    }

    @Tag("tag")
    private static class TestAbstractField<T>
            extends AbstractField<TestAbstractField<T>, T> {

        public TestAbstractField() {
            this(null);
        }

        public TestAbstractField(T defaultValue) {
            super(defaultValue);
        }

        T presentationValue;

        SerializableConsumer<T> setPresentationValue = value -> presentationValue = value;

        SerializableBiPredicate<T, T> valueEquals;
        SerializableSupplier<T> emptyValue;

        @Override
        protected void setPresentationValue(T value) {
            setPresentationValue.accept(value);
        }

        @Override
        protected boolean valueEquals(T value1, T value2) {
            if (valueEquals != null) {
                return valueEquals.test(value1, value2);
            } else {
                return super.valueEquals(value1, value2);
            }
        }

        @Override
        public T getEmptyValue() {
            if (emptyValue != null) {
                return emptyValue.get();
            } else {
                return super.getEmptyValue();
            }
        }

        public void updatePresentationValue(T value, boolean fromClient) {
            this.presentationValue = value;
            setModelValue(value, fromClient);
        }

        @Override
        public void setModelValue(T value, boolean fromClient) {
            super.setModelValue(value, fromClient);
        }

        public void valueUpdatedFromClient(boolean fromClient) {
            super.setModelValue(presentationValue, fromClient);
        }
    }

    private static void assertNoEvents(HasValue<?, ?> observable) {
        observable.addValueChangeListener(
                event -> Assert.fail("Got unexpected event: " + event));
    }

    @Test
    public void initialValue_used() {
        TestAbstractField<String> field = new TestAbstractField<>("foo");

        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void emptyValue_sameAsInitial() {
        TestAbstractField<String> field = new TestAbstractField<>("foo");

        Assert.assertEquals("foo", field.getEmptyValue());

        field.setValue("bar");

        Assert.assertEquals(
                "Empty value shouldn't change when value is changed", "foo",
                field.getEmptyValue());
    }

    @Test
    public void initialValue_defaultNull() {
        TestAbstractField<String> field = new TestAbstractField<>();

        Assert.assertNull(field.getValue());
        Assert.assertNull(field.getEmptyValue());
    }

    @Test
    public void setValue_differentValue_firesOneEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);

        field.setValue("Foo");

        eventMonitor.assertEvent(false, null, "Foo");
        Assert.assertEquals("Foo", field.presentationValue);
    }

    @Test
    public void setValue_sameValue_firesNoEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);

        field.setValue(field.getValue());

        eventMonitor.assertNoEvent();
    }

    @Test
    public void clear_firesIfNotEmpty() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);

        field.clear();
        eventMonitor.assertNoEvent();

        field.setValue("foo");
        eventMonitor.discard();
        Assert.assertFalse(field.isEmpty());

        field.clear();
        eventMonitor.assertEvent(false, "foo", null);
        Assert.assertTrue(field.isEmpty());

        field.clear();
        eventMonitor.assertNoEvent();
    }

    @Test
    public void clear_customEmptyValue_emptyValueUsed() {
        TestAbstractField<String> field = new TestAbstractField<>();
        field.emptyValue = () -> "";

        Assert.assertFalse(field.isEmpty());

        field.clear();
        Assert.assertTrue(field.isEmpty());
        Assert.assertEquals("", field.getValue());
    }

    @Test
    public void updateFromClient_differentValue_updatesAndFires() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);

        field.updatePresentationValue("foo", true);
        eventMonitor.assertEvent(true, null, "foo");

        field.updatePresentationValue("foo", true);
        eventMonitor.assertNoEvent();
    }

    @Test
    public void customEquals() {
        TestAbstractField<Integer> field = new TestAbstractField<>();
        field.valueEquals = (value1, value2) -> value1 == value2;

        Integer value1 = new Integer(0);
        Integer value2 = new Integer(0);

        field.setValue(value1);

        ValueChangeMonitor<Integer> eventMonitor = new ValueChangeMonitor<>(
                field);

        field.setValue(value1);
        eventMonitor.assertNoEvent();

        field.setValue(value2);
        eventMonitor.assertEvent(false, value1, value2);

        field.updatePresentationValue(value2, true);
        eventMonitor.assertNoEvent();

        field.updatePresentationValue(value1, true);
        eventMonitor.assertEvent(true, value2, value1);
    }

    @Test
    public void customEquals_isEmpty() {
        Integer value1 = new Integer(0);
        Integer value2 = new Integer(0);

        TestAbstractField<Integer> field = new TestAbstractField<>(value1);
        field.valueEquals = (v1, v2) -> v1 == v2;

        Assert.assertTrue(field.isEmpty());
        Assert.assertFalse(field.getOptionalValue().isPresent());

        field.setValue(value2);
        Assert.assertFalse(field.isEmpty());
        Assert.assertTrue(field.getOptionalValue().isPresent());

        field.clear();
        Assert.assertTrue(field.isEmpty());
        Assert.assertFalse(field.getOptionalValue().isPresent());
    }

    @Test
    public void getValue_changesAfterUpdatedFromClient() {
        TestAbstractField<String> field = new TestAbstractField<>();
        Assert.assertNull(field.getValue());

        field.presentationValue = "foo";
        Assert.assertNull(field.getValue());

        field.valueUpdatedFromClient(false);
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void setPresentation_setSameValue_notRunAgain() {
        TestAbstractField<String> field = new TestAbstractField<>();
        AtomicReference<String> lastWriteValue = new AtomicReference<>();
        field.setPresentationValue = value -> {
            Assert.assertNull("Unexpected update",
                    lastWriteValue.getAndSet(value));
            field.presentationValue = value;
        };

        field.setValue("foo");
        Assert.assertEquals("foo", lastWriteValue.get());

        lastWriteValue.set(null);

        field.setValue("foo");
        Assert.assertNull(lastWriteValue.get());
    }

    @Test
    public void updatePresentation_doesntCallSetPresentation() {
        TestAbstractField<String> field = new TestAbstractField<>();
        field.setPresentationValue = value -> Assert
                .fail("setPresentationValue should not run");

        field.updatePresentationValue("foo", true);
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void setPresentation_throws_sameException_valuePreserved() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);

        field.setPresentationValue = value -> {
            throw new IllegalStateException(value);
        };

        try {
            field.setValue("foo");
            Assert.fail("Exception should have been thrown");
        } catch (IllegalStateException e) {
            Assert.assertEquals("foo", e.getMessage());
        }

        eventMonitor.assertNoEvent();
        Assert.assertNull(field.getValue());
    }

    @Test
    public void setPresentation_partialUpdates_onlyOneEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);

        field.setPresentationValue = value -> {
            /*
             * Emulate a situation where multiple element properties are
             * modified, each firing its own value change event
             */
            field.updatePresentationValue("temp value", true);
            field.updatePresentationValue(value, true);
        };

        field.setValue("foo");
        eventMonitor.assertEvent(false, null, "foo");
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void setPresentation_changesValue_onlyOneEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);

        field.setPresentationValue = value -> {
            field.setPresentationValue = value2 -> Assert
                    .fail("setPresentationValue should not be called again");

            field.updatePresentationValue(value.toUpperCase(Locale.ROOT),
                    false);
        };

        field.setValue("foo");
        eventMonitor.assertEvent(false, null, "FOO");
        Assert.assertEquals("FOO", field.getValue());
    }

    @Test
    public void setPresentation_revertsValue_noEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);

        field.setPresentationValue = value -> {
            field.setPresentationValue = value2 -> Assert
                    .fail("setPresentationValue should not be called again");
            field.valueUpdatedFromClient(false);
        };

        field.setValue("foo");
        eventMonitor.assertNoEvent();

        Assert.assertNull(field.getValue());
    }

    @Test
    public void setValueInEventHandler() {
        TestAbstractField<String> field = new TestAbstractField<>();

        List<ValueChangeEvent<String>> beforeEvents = new ArrayList<>();
        List<ValueChangeEvent<String>> afterEvents = new ArrayList<>();

        field.addValueChangeListener(beforeEvents::add);
        field.addValueChangeListener(event -> {
            event.getSource().setValue("bar");
        });
        field.addValueChangeListener(afterEvents::add);

        field.setValue("foo");

        Assert.assertEquals(2, beforeEvents.size());
        ValueChangeMonitor.assertEventValues(beforeEvents.get(0), null, "foo");
        ValueChangeMonitor.assertEventValues(beforeEvents.get(1), "foo", "bar");

        // Does not make sense, but still testing so we know how it works
        // Also, this is how Vaadin 8 works, and nobody has been too upset
        Assert.assertEquals(2, afterEvents.size());
        ValueChangeMonitor.assertEventValues(afterEvents.get(0), "foo", "bar");
        ValueChangeMonitor.assertEventValues(afterEvents.get(1), null, "foo");
    }

    @Test
    public void requiredIndicator_writtenToElement() {
        TestAbstractField<String> field = new TestAbstractField<>();
        Element element = field.getElement();

        Assert.assertFalse(element.getProperty("required", false));

        field.setRequiredIndicatorVisible(true);
        Assert.assertTrue(element.getProperty("required", false));

        field.setRequiredIndicatorVisible(false);
        Assert.assertFalse(element.getProperty("required", false));
    }

    @Test
    public void requiredIndicator_readFromElement() {
        TestAbstractField<String> field = new TestAbstractField<>();
        Element element = field.getElement();

        Assert.assertFalse(field.isRequiredIndicatorVisible());

        element.setProperty("required", true);
        Assert.assertTrue(field.isRequiredIndicatorVisible());

        element.setProperty("required", false);
        Assert.assertFalse(field.isRequiredIndicatorVisible());
    }

    @Test
    public void readonly_writtenToElement() {
        TestAbstractField<String> field = new TestAbstractField<>();
        Element element = field.getElement();

        Assert.assertFalse(element.getProperty("readonly", false));

        field.setReadOnly(true);
        Assert.assertTrue(element.getProperty("readonly", false));

        field.setReadOnly(false);
        Assert.assertFalse(element.getProperty("readonly", false));
    }

    @Test
    public void readonly_readFromElement() {
        TestAbstractField<String> field = new TestAbstractField<>();
        Element element = field.getElement();

        Assert.assertFalse(field.isReadOnly());

        element.setProperty("readonly", true);
        Assert.assertTrue(field.isReadOnly());

        element.setProperty("readonly", false);
        Assert.assertFalse(field.isReadOnly());
    }

    @Test
    public void readonly_setValue_accepted() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);
        field.setReadOnly(true);

        field.setValue("foo");

        eventMonitor.discard();
        Assert.assertEquals("foo", field.presentationValue);
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void readonly_presentationFromClient_reverted() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);
        field.setReadOnly(true);

        field.updatePresentationValue("foo", true);

        eventMonitor.assertNoEvent();
        Assert.assertEquals(null, field.getValue());
        Assert.assertEquals(null, field.presentationValue);
    }

    @Test
    public void readonly_presentationFromServer_accepted() {
        TestAbstractField<String> field = new TestAbstractField<>();
        ValueChangeMonitor<String> eventMonitor = new ValueChangeMonitor<>(
                field);
        field.setReadOnly(true);

        field.updatePresentationValue("foo", false);

        eventMonitor.discard();
        Assert.assertEquals("foo", field.presentationValue);
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void noOwnPublicApi() {
        List<Method> newPublicMethods = PublicApiAnalyzer
                .findNewPublicMethods(AbstractField.class)
                .collect(Collectors.toList());
        Assert.assertEquals(Collections.emptyList(), newPublicMethods);
    }

    @Test
    public void serializable() {
        TestAbstractField<String> field = new TestAbstractField<>();
        field.addValueChangeListener(ignore -> {
        });
        field.setValue("foo");

        TestAbstractField<String> anotherField = SerializationUtils
                .roundtrip(field);
        Assert.assertEquals("foo", anotherField.getValue());
    }

}
