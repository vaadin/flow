/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Objects;

import com.vaadin.flow.data.binder.testcomponents.TestLabel;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.shared.Registration;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ReadOnlyHasValueTest {
    private static final String SAY_SOMETHING = "Say something";
    private static final String SAY_SOMETHING_ELSE = "Say something else";
    private static final String NO_VALUE = "-no-value-";
    private TestLabel label;
    private ReadOnlyHasValue<String> hasValue;

    @Before
    public void setup() {
        label = new TestLabel();
        hasValue = new ReadOnlyHasValue<>(label::setText);
    }

    @Test
    public void testBase() {
        hasValue.setReadOnly(true);
        hasValue.setRequiredIndicatorVisible(false);
        Registration registration = hasValue.addValueChangeListener(e -> {
        });
        registration.remove();
        hasValue.setValue(SAY_SOMETHING);
        assertEquals(SAY_SOMETHING, hasValue.getValue());
        assertEquals(SAY_SOMETHING, label.getText());
        hasValue.setValue(SAY_SOMETHING_ELSE);
        assertEquals(SAY_SOMETHING_ELSE, hasValue.getValue());
        assertEquals(SAY_SOMETHING_ELSE, label.getText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRO() {
        hasValue.setReadOnly(false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIndicator() {
        hasValue.setRequiredIndicatorVisible(true);
    }

    @Test
    public void testBind() {
        Binder<Bean> beanBinder = new Binder<>(Bean.class);
        TestLabel label = new TestLabel();
        ReadOnlyHasValue<Long> intHasValue = new ReadOnlyHasValue<>(
                i -> label.setText(Objects.toString(i, "")));

        beanBinder.forField(intHasValue).bind("v");

        beanBinder.readBean(new Bean(42));
        assertEquals("42", label.getText());
        assertEquals(42L, intHasValue.getValue().longValue());

        Registration registration = intHasValue.addValueChangeListener(e -> {
            assertEquals(42L, e.getOldValue().longValue());
            assertSame(intHasValue, e.getHasValue());
            assertFalse(e.isFromClient());
        });
        beanBinder.readBean(new Bean(1984));
        assertEquals("1984", label.getText());
        assertEquals(1984L, intHasValue.getValue().longValue());

        registration.remove();

        beanBinder.readBean(null);
        assertEquals("", label.getText());
        assertEquals(null, intHasValue.getValue());

    }

    @Test
    public void testEmptyValue() {
        Binder<Bean> beanBinder = new Binder<>(Bean.class);
        TestLabel label = new TestLabel();
        ReadOnlyHasValue<String> strHasValue = new ReadOnlyHasValue<>(
                label::setText, NO_VALUE);

        beanBinder.forField(strHasValue)
                .withConverter(Long::parseLong, (Long i) -> "" + i).bind("v");

        beanBinder.readBean(new Bean(42));
        assertEquals("42", label.getText());

        beanBinder.readBean(null);
        assertEquals(NO_VALUE, label.getText());
        assertTrue(strHasValue.isEmpty());
    }

    public static class Bean {
        public Bean(long v) {
            this.v = v;
        }

        private long v;

        public long getV() {
            return v;
        }

        public void setV(long v) {
            this.v = v;
        }
    }
}
