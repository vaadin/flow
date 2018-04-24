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
package com.vaadin.flow.component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.dom.Element;

public class AbstractFieldTest {
    // This isn't a test in itself, but it shows that no more than two methods
    // need to be overridden
    private static class SimpleAbstractField<T>
            extends AbstractField<SimpleAbstractField<T>, T> {
        @Override
        protected void writeValue(T value) {
        }

        @Override
        protected T readValue() {
            return null;
        }
    }

    @Tag("tag")
    private static class TestAbstractField<T>
            extends AbstractField<TestAbstractField<T>, T> {
        T internalValue;

        Consumer<T> writeValue = value -> internalValue = value;
        Supplier<T> readValue = () -> internalValue;

        BiPredicate<T, T> valueEquals;
        Boolean mayReadValue = null;
        T emptyValue;

        @Override
        protected void writeValue(T value) {
            writeValue.accept(value);
        }

        @Override
        protected T readValue() {
            return readValue.get();
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
        protected boolean mayReadValue() {
            if (mayReadValue != null) {
                return mayReadValue.booleanValue();
            } else {
                return super.mayReadValue();
            }
        }

        @Override
        public T getEmptyValue() {
            return emptyValue;
        }

        @Override
        public boolean valueUpdatedFromClient() {
            return super.valueUpdatedFromClient();
        }
    }

    private static class EventMonitor<T> {

        private TestAbstractField<T> observable;

        public EventMonitor(TestAbstractField<T> obserable) {
            this.observable = obserable;

            obserable.addValueChangeListener(event -> {
                if (capturedEvent != null) {
                    Assert.fail("There is already an event. Old event: "
                            + capturedEvent + ", new event: " + event);
                }

                Assert.assertSame(obserable, event.getSource());

                capturedEvent = event;
            });
        }

        ValueChangeEvent<TestAbstractField<T>, T> capturedEvent;

        public void discard() {
            Assert.assertNotNull("There should be an event", capturedEvent);
            capturedEvent = null;
        }

        public void assertEvent(boolean fromClient, T oldValue, T newValue) {
            Assert.assertNotNull("There should be an event", capturedEvent);
            Assert.assertTrue(fromClient == capturedEvent.isFromClient());

            assertEventValues(capturedEvent, oldValue, newValue);

            discard();
        }

        public void assertNoEvent() {
            Assert.assertNull("There should be no event", capturedEvent);
        }
    }

    private static <T> void assertEventValues(ValueChangeEvent<?, T> event,
            T oldValue, T newValue) {
        Assert.assertEquals(oldValue, event.getOldValue());
        Assert.assertEquals(newValue, event.getValue());
    }

    private static void assertNoEvents(HasValue<?, ?> observable) {
        observable.addValueChangeListener(
                event -> Assert.fail("Got unexpected event: " + event));
    }

    @Test
    public void setValue_differentValue_firesOneEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);

        field.setValue("Foo");

        eventMonitor.assertEvent(false, null, "Foo");
        Assert.assertEquals("Foo", field.internalValue);
    }

    @Test
    public void setValue_sameValue_firesNoEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);

        field.setValue(field.getValue());

        eventMonitor.assertNoEvent();
    }

    @Test
    public void clear_firesIfNotEmpty() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);

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
        field.emptyValue = "";

        Assert.assertFalse(field.isEmpty());

        field.clear();
        Assert.assertTrue(field.isEmpty());
        Assert.assertEquals("", field.getValue());
    }

    @Test
    public void updateFromClient_differentValue_updatesAndFires() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);

        field.internalValue = "foo";
        field.valueUpdatedFromClient();
        eventMonitor.assertEvent(true, null, "foo");

        field.valueUpdatedFromClient();
        eventMonitor.assertNoEvent();
    }

    @Test
    public void customEquals() {
        TestAbstractField<Integer> field = new TestAbstractField<>();
        field.valueEquals = (value1, value2) -> value1 == value2;

        Integer value1 = new Integer(0);
        Integer value2 = new Integer(0);

        field.setValue(value1);

        EventMonitor<Integer> eventMonitor = new EventMonitor<>(field);

        field.setValue(value1);
        eventMonitor.assertNoEvent();

        field.setValue(value2);
        eventMonitor.assertEvent(false, value1, value2);

        field.internalValue = value2;
        field.valueUpdatedFromClient();
        eventMonitor.assertNoEvent();

        field.internalValue = value1;
        field.valueUpdatedFromClient();
        eventMonitor.assertEvent(true, value2, value1);
    }

    @Test
    public void initialValue_mayNoReadValue_emptyValue() {
        TestAbstractField<String> field = new TestAbstractField<>();
        field.mayReadValue = Boolean.FALSE;
        field.emptyValue = "foo";

        Assert.assertSame("foo", field.getValue());
        Assert.assertTrue(field.isEmpty());
    }

    @Test
    public void mayReadValue_protectsReadValue() {
        TestAbstractField<Integer> field = new TestAbstractField<>();
        EventMonitor<Integer> eventMonitor = new EventMonitor<>(field);

        AtomicInteger readValueCount = new AtomicInteger();
        field.readValue = () -> Integer
                .valueOf(readValueCount.incrementAndGet());

        field.mayReadValue = Boolean.FALSE;
        field.valueUpdatedFromClient();
        Assert.assertEquals(0, readValueCount.get());
        eventMonitor.assertNoEvent();

        field.mayReadValue = Boolean.TRUE;
        field.valueUpdatedFromClient();
        Assert.assertEquals(1, readValueCount.get());
        eventMonitor.assertEvent(true, null, 1);
    }

    @Test
    public void getValue_changesAfterUpdatedFromClient() {
        TestAbstractField<String> field = new TestAbstractField<>();
        Assert.assertNull(field.getValue());

        field.internalValue = "foo";
        Assert.assertNull(field.getValue());

        field.valueUpdatedFromClient();
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void updateFromClient_changedValue_returnsTrue() {
        TestAbstractField<String> field = new TestAbstractField<>();
        // Init internal value
        field.valueUpdatedFromClient();

        field.internalValue = "foo";
        boolean valueUpdated = field.valueUpdatedFromClient();

        Assert.assertTrue(valueUpdated);
    }

    @Test
    public void updateFromClient_sameValue_returnsFalse() {
        TestAbstractField<String> field = new TestAbstractField<>();
        // Init internal value
        field.valueUpdatedFromClient();

        boolean valueUpdated = field.valueUpdatedFromClient();

        Assert.assertFalse(valueUpdated);
    }

    @Test
    public void updateFromClient_notMayRead_returnsFalse() {
        TestAbstractField<String> field = new TestAbstractField<>();
        // Init internal value
        field.valueUpdatedFromClient();

        field.internalValue = "foo";
        field.mayReadValue = Boolean.FALSE;
        boolean valueUpdated = field.valueUpdatedFromClient();

        Assert.assertFalse(valueUpdated);
    }

    @Test
    public void writeValue_setSameValue_notRunAgain() {
        TestAbstractField<String> field = new TestAbstractField<>();
        AtomicReference<String> lastWriteValue = new AtomicReference<>();
        field.writeValue = value -> {
            Assert.assertNull("Unexpected write",
                    lastWriteValue.getAndSet(value));
            field.internalValue = value;
        };

        field.setValue("foo");
        Assert.assertEquals("foo", lastWriteValue.get());

        lastWriteValue.set(null);

        field.setValue("foo");
        Assert.assertNull(lastWriteValue.get());
    }

    @Test
    public void writeValue_setClientValue_notRun() {
        TestAbstractField<String> field = new TestAbstractField<>();
        field.writeValue = value -> Assert.fail("writeValue should not run");

        field.internalValue = "foo";
        field.valueUpdatedFromClient();
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void writeValue_throws_sameException_valuePreserved() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);

        field.writeValue = value -> {
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
    public void writeValue_partialUpdates_onlyOneEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);

        field.writeValue = value -> {
            /*
             * Emulate a situation where multiple element properties are
             * modified, each firing its own value change event
             */
            field.internalValue = "temp value";
            field.valueUpdatedFromClient();
            field.internalValue = value;
            field.valueUpdatedFromClient();
        };

        field.setValue("foo");
        eventMonitor.assertEvent(false, null, "foo");
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void writeValue_rewritesValue_onlyOneEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);

        field.writeValue = value -> {
            field.writeValue = value2 -> Assert
                    .fail("writeValue should not be called again");

            field.internalValue = value.toUpperCase(Locale.ROOT);
            field.valueUpdatedFromClient();
        };

        field.setValue("foo");
        eventMonitor.assertEvent(false, null, "FOO");
        Assert.assertEquals("FOO", field.getValue());
    }

    @Test
    public void writeValue_changesBack_noEvent() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);

        field.writeValue = value -> {
            field.writeValue = value2 -> Assert
                    .fail("writeValue should not be called again");
            field.valueUpdatedFromClient();
        };

        field.setValue("foo");
        eventMonitor.assertNoEvent();

        Assert.assertNull(field.getValue());
    }

    @Test
    public void readValue_throws_worksUntilClientUpdate() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);

        field.readValue = () -> {
            throw new IllegalStateException(field.internalValue);
        };

        Assert.assertNull(field.getValue());

        field.setValue("foo");
        eventMonitor.discard();
        Assert.assertEquals("foo", field.getValue());

        field.internalValue = "bar";
        try {
            field.valueUpdatedFromClient();
            Assert.fail("Exception should have been thrown");
        } catch (IllegalStateException e) {
            Assert.assertEquals("bar", e.getMessage());
        }

        eventMonitor.assertNoEvent();
        Assert.assertEquals("value should not have changed", "foo",
                field.getValue());
    }

    @Test
    public void setValueInEventHandler() {
        TestAbstractField<String> field = new TestAbstractField<>();

        List<ValueChangeEvent<TestAbstractField<String>, String>> beforeEvents = new ArrayList<>();
        List<ValueChangeEvent<TestAbstractField<String>, String>> afterEvents = new ArrayList<>();

        field.addValueChangeListener(beforeEvents::add);
        field.addValueChangeListener(event -> {
            event.getSource().setValue("bar");
        });
        field.addValueChangeListener(afterEvents::add);

        field.setValue("foo");

        Assert.assertEquals(2, beforeEvents.size());
        assertEventValues(beforeEvents.get(0), null, "foo");
        assertEventValues(beforeEvents.get(1), "foo", "bar");

        // Does not make sense, but still testing so we know how it works
        // Also, this is how Vaadin 8 works, and nobody has been too upset
        Assert.assertEquals(2, afterEvents.size());
        assertEventValues(afterEvents.get(0), "foo", "bar");
        assertEventValues(afterEvents.get(1), null, "foo");
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
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);
        field.setReadOnly(true);

        field.setValue("foo");

        eventMonitor.discard();
        Assert.assertEquals("foo", field.internalValue);
        Assert.assertEquals("foo", field.getValue());
    }

    @Test
    public void readonly_clientValue_reverted() {
        TestAbstractField<String> field = new TestAbstractField<>();
        EventMonitor<String> eventMonitor = new EventMonitor<>(field);
        field.setReadOnly(true);

        field.internalValue = "foo";
        field.valueUpdatedFromClient();

        eventMonitor.assertNoEvent();
        Assert.assertEquals(null, field.getValue());
        Assert.assertEquals(null, field.internalValue);
    }

    @Test
    public void noOwnPublicApi() {
        for (Method method : AbstractField.class.getMethods()) {
            if (method.getDeclaringClass() == AbstractField.class) {
                boolean matchInSupertype = Stream
                        .concat(Stream.of(AbstractField.class.getSuperclass()),
                                Stream.of(AbstractField.class.getInterfaces()))
                        .anyMatch(superType -> {
                            try {
                                superType.getMethod(method.getName(),
                                        method.getParameterTypes());
                                return true;
                            } catch (NoSuchMethodException ignore) {
                                return false;
                            }
                        });
                if (!matchInSupertype) {
                    Assert.fail("Own public method: " + method);
                }
            }
        }
    }

}
