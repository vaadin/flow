/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.tests.PublicApiAnalyzer;

public class AbstractSinglePropertyFieldTest {
    @Tag("tag")
    static class StringField
            extends AbstractSinglePropertyField<StringField, String> {
        public StringField() {
            super("property", "", false);
        }

        // Exposed as public for testing purposes
        @Override
        public void setSynchronizedEvent(String synchronizedEventName) {
            super.setSynchronizedEvent(synchronizedEventName);
        }

        List<String> getSynchronizedEvents() {
            return getElement().getSynchronizedPropertyEvents()
                    .collect(Collectors.toList());
        }
    }

    @Test
    public void stringField_basicCases() {
        StringField field = new StringField();
        ValueChangeMonitor<String> monitor = new ValueChangeMonitor<>(field);

        Assert.assertEquals("", field.getValue());
        Assert.assertFalse(field.getElement().hasProperty("property"));
        monitor.assertNoEvent();

        field.setValue("foo");
        Assert.assertEquals("foo", field.getElement().getProperty("property"));
        monitor.assertEvent(false, "", "foo");

        field.getElement().setProperty("property", "bar");
        Assert.assertEquals("bar", field.getValue());
        monitor.assertEvent(false, "foo", "bar");

        // Cannot do removeProperty because
        // https://github.com/vaadin/flow/issues/3994
        field.getElement().setProperty("property", null);
        Assert.assertEquals("", field.getValue());
        monitor.assertEvent(false, "bar", "");
    }

    @Test(expected = NullPointerException.class)
    public void stringField_setValueNull_exceptionAndNoEvent() {
        StringField field = new StringField();
        ValueChangeMonitor<String> monitor = new ValueChangeMonitor<>(field);

        try {
            field.setValue(null);
        } finally {
            monitor.assertNoEvent();
        }
    }

    @Test
    public void stringField_initProperty_noEvent() {
        StringField field = new StringField();
        ValueChangeMonitor<String> monitor = new ValueChangeMonitor<>(field);

        field.getElement().setProperty("property", "");

        monitor.assertNoEvent();
    }

    @Test
    public void synchronizedProperty_default() {
        StringField stringField = new StringField();

        List<String> synchronizedProperties = stringField
                .getSynchronizedEvents();
        Assert.assertEquals(Arrays.asList("property-changed"),
                synchronizedProperties);
    }

    @Test
    public void synchronizedProperty_redefined() {
        StringField stringField = new StringField();
        stringField.setSynchronizedEvent("blur");

        List<String> synchronizedProperties = stringField
                .getSynchronizedEvents();
        Assert.assertEquals(Arrays.asList("blur"), synchronizedProperties);
    }

    @Test
    public void synchronizedProperty_null_noSynchronization() {
        StringField stringField = new StringField();
        stringField.setSynchronizedEvent(null);

        List<String> synchronizedProperties = stringField
                .getSynchronizedEvents();
        Assert.assertEquals(Arrays.asList(), synchronizedProperties);
    }

    @Tag("tag")
    private static class StringNullField
            extends AbstractSinglePropertyField<StringNullField, String> {
        public StringNullField() {
            super("property", null, true);
        }
    }

    @Test
    public void stringNullField_basicCases() {
        StringNullField field = new StringNullField();
        ValueChangeMonitor<String> monitor = new ValueChangeMonitor<>(field);

        Assert.assertEquals(null, field.getValue());
        Assert.assertFalse(field.getElement().hasProperty("property"));
        monitor.assertNoEvent();

        field.getElement().setProperty("property", "");
        Assert.assertEquals("", field.getValue());
        monitor.assertEvent(false, null, "");

        field.setValue(null);
        Assert.assertFalse(field.getElement().hasProperty("property"));
        monitor.assertEvent(false, "", null);
    }

    @Tag("tag")
    private static class DoubleField
            extends AbstractSinglePropertyField<DoubleField, Double> {
        public DoubleField() {
            super("property", Double.valueOf(0), false);
        }
    }

    @Test
    public void doubleField_basicCases() {
        DoubleField field = new DoubleField();
        ValueChangeMonitor<Double> monitor = new ValueChangeMonitor<>(field);

        Assert.assertEquals(0.0, field.getValue(), 0);
        Assert.assertFalse(field.getElement().hasProperty("property"));
        monitor.assertNoEvent();

        field.setValue(10.1);
        Assert.assertEquals(10.1,
                field.getElement().getProperty("property", 0.0), 0);
        monitor.assertEvent(false, 0.0, 10.1);

        field.getElement().setProperty("property", 1.1);
        Assert.assertEquals(1.1, field.getValue(), 0);
        monitor.assertEvent(false, 10.1, 1.1);

        // Cannot do removeProperty because
        // https://github.com/vaadin/flow/issues/3994
        field.getElement().setProperty("property", null);
        Assert.assertEquals(0.0, field.getValue(), 0);
        monitor.assertEvent(false, 1.1, 0.0);
    }

    @Tag("tag")
    private static class IntegerField
            extends AbstractSinglePropertyField<IntegerField, Integer> {
        public IntegerField() {
            super("property", Integer.valueOf(42), false);
        }
    }

    @Test
    public void integerField_basicCases() {
        IntegerField field = new IntegerField();
        ValueChangeMonitor<Integer> monitor = new ValueChangeMonitor<>(field);

        Assert.assertEquals(42, field.getValue().intValue());
        Assert.assertFalse(field.getElement().hasProperty("property"));
        monitor.assertNoEvent();

        field.setValue(0);
        Assert.assertEquals(0, field.getElement().getProperty("property", -1));
        monitor.assertEvent(false, 42, 0);

        field.getElement().setProperty("property", 1);
        Assert.assertEquals(1, field.getValue().intValue());
        monitor.assertEvent(false, 0, 1);

        // Cannot do removeProperty because
        // https://github.com/vaadin/flow/issues/3994
        field.getElement().setProperty("property", null);
        Assert.assertEquals(42, field.getValue().intValue());
        monitor.assertEvent(false, 1, 42);
    }

    @Tag("tag")
    private static class BooleanField
            extends AbstractSinglePropertyField<BooleanField, Boolean> {
        public BooleanField() {
            super("property", Boolean.FALSE, false);
        }
    }

    @Test
    public void booleanField_basicCases() {
        BooleanField field = new BooleanField();
        ValueChangeMonitor<Boolean> monitor = new ValueChangeMonitor<>(field);

        Assert.assertFalse(field.getValue());
        Assert.assertFalse(field.getElement().hasProperty("property"));
        monitor.assertNoEvent();

        field.setValue(true);
        Assert.assertTrue(field.getElement().getProperty("property", false));
        monitor.assertEvent(false, false, true);

        field.getElement().setProperty("property", false);
        Assert.assertFalse(field.getValue());
        monitor.assertEvent(false, true, false);

        // Set value to true again so we can test that null -> false
        field.setValue(true);
        monitor.discard();

        // Cannot do removeProperty because
        // https://github.com/vaadin/flow/issues/3994
        field.getElement().setProperty("property", null);
        Assert.assertFalse(field.getValue());
        monitor.assertEvent(false, true, false);
    }

    @Tag("tag")
    // Broken since AbstractSinglePropertyField cannot know how to store
    // LocalDate instances in an element property
    private static class SimpleDateField
            extends AbstractSinglePropertyField<SimpleDateField, LocalDate> {
        public SimpleDateField() {
            super("property", null, true);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void simpleDateField_constructor_throws() {
        new SimpleDateField();
    }

    @Tag("tag")
    private static class DateField
            extends AbstractSinglePropertyField<DateField, LocalDate> {
        public DateField() {
            super("property", null, String.class, LocalDate::parse,
                    LocalDate::toString);
        }
    }

    @Test
    public void dateField_basicCases() {
        DateField field = new DateField();
        ValueChangeMonitor<LocalDate> monitor = new ValueChangeMonitor<>(field);

        Assert.assertEquals(null, field.getValue());
        Assert.assertFalse(field.getElement().hasProperty("property"));
        monitor.assertNoEvent();

        field.setValue(LocalDate.of(2018, 4, 25));
        Assert.assertEquals("2018-04-25",
                field.getElement().getProperty("property"));
        monitor.assertEvent(false, null, LocalDate.of(2018, 4, 25));

        field.getElement().setProperty("property", "2017-03-24");
        Assert.assertEquals(LocalDate.of(2017, 3, 24), field.getValue());
        monitor.assertEvent(false, LocalDate.of(2018, 4, 25),
                LocalDate.of(2017, 3, 24));

        // Cannot do removeProperty because
        // https://github.com/vaadin/flow/issues/3994
        field.getElement().setProperty("property", null);
        Assert.assertEquals(null, field.getValue());
        monitor.assertEvent(false, LocalDate.of(2017, 3, 24), null);
    }

    @Tag("tag")
    private static class IntegerToStringField
            extends AbstractSinglePropertyField<IntegerToStringField, Integer> {
        public IntegerToStringField() {
            super("property", null, String.class, Integer::new,
                    String::valueOf);
        }

        @Override
        protected boolean hasValidValue() {
            return getElement().getProperty("property", "").matches("[0-9]*");
        }
    }

    @Test
    public void integerToString_basicCases() {
        IntegerToStringField field = new IntegerToStringField();
        ValueChangeMonitor<Integer> monitor = new ValueChangeMonitor<>(field);
        Assert.assertNull(field.getValue());

        // Verify base cases
        field.getElement().setProperty("property", "10");
        Assert.assertEquals(10, field.getValue().intValue());
        monitor.assertEvent(false, null, 10);

        // Verify base cases
        field.getElement().setProperty("property", null);
        Assert.assertNull(field.getValue());
        monitor.assertEvent(false, 10, null);

        field.setValue(20);
        Assert.assertEquals("20", field.getElement().getProperty("property"));
        monitor.assertEvent(false, null, 20);
    }

    @Test
    public void integerToString_nonIntegerInput_ignore() {
        IntegerToStringField field = new IntegerToStringField();
        ValueChangeMonitor<Integer> monitor = new ValueChangeMonitor<>(field);

        field.getElement().setProperty("property", "Not a number");
        monitor.assertNoEvent();
        Assert.assertNull(field.getValue());
        Assert.assertEquals("Unparseable should not affect property",
                "Not a number", field.getElement().getProperty("property"));

        field.setValue(10);
        monitor.assertEvent(false, null, 10);
        Assert.assertEquals("setValue should override unparseable property",
                "10", field.getElement().getProperty("property"));
    }

    @Test
    public void noOwnPublicApi() {
        List<Method> newPublicMethods = PublicApiAnalyzer
                .findNewPublicMethods(AbstractSinglePropertyField.class)
                .collect(Collectors.toList());
        Assert.assertEquals(Collections.emptyList(), newPublicMethods);
    }

}
