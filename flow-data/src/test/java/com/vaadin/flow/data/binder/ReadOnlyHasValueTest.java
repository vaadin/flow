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
