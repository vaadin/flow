/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.component;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.router.RouterLink;
import org.junit.Assert;
import org.junit.Test;

public class HasValueTest {

    private class HasValueImp extends RouterLink
            implements HasValue<RouterLink, String> {

        @Override
        public void setValue(String value) {
            get().getElement().setProperty(getClientValuePropertyName(), value);
        }

        @Override
        public String getValue() {
            return getElement().getProperty(getClientValuePropertyName(),
                    getEmptyValue());
        }
    }

    private class HasValueWithNonNullEmptyValue extends HasValueImp {
        @Override
        public String getEmptyValue() {
            return "";
        }
    }

    private HasValue<?, String> component;

    @Test
    public void testValueChangeListener_valueChangedFromServer_triggersEvent() {
        component = new HasValueImp();

        AtomicReference<HasValue.ValueChangeEvent> reference = new AtomicReference<>();
        component.addValueChangeListener(reference::set);

        component.setValue(null);
        Assert.assertNull("no event should be triggered yet", reference.get());

        component.setValue("foobar");

        Assert.assertNotNull("event should have been triggered",
                reference.get());
        HasValue.ValueChangeEvent event = reference.get();

        Assert.assertNull("invalid previous value", event.getOldValue());
        Assert.assertEquals("invalid value", "foobar", event.getValue());

        component.clear();

        event = reference.get();

        Assert.assertEquals("invalid previous value", "foobar",
                event.getOldValue());
        Assert.assertNull("invalid value", event.getValue());
    }

    @Test
    public void testValueChangeListener_nonNullDefaultValue_valueChangedFromServer_triggersEvent() {
        component = new HasValueWithNonNullEmptyValue();

        AtomicReference<HasValue.ValueChangeEvent> reference = new AtomicReference<>();
        component.addValueChangeListener(reference::set);

        component.setValue(null);
        Assert.assertNull("no event should be triggered yet", reference.get());

        component.setValue("foobar");

        Assert.assertNotNull("event should have been triggered",
                reference.get());
        HasValue.ValueChangeEvent event = reference.get();

        Assert.assertEquals("invalid previous value", "", event.getOldValue());
        Assert.assertEquals("invalid value", "foobar", event.getValue());

        component.clear();

        event = reference.get();

        Assert.assertEquals("invalid previous value", "foobar",
                event.getOldValue());
        Assert.assertEquals("invalid value", "", event.getValue());
    }

    @Test
    public void testValueChangeListener_nonNullDefaultValue_initialValueChangedFromNullToEmpty_noEvent() {
        component = new HasValueWithNonNullEmptyValue();

        AtomicReference<HasValue.ValueChangeEvent> reference = new AtomicReference<>();
        component.addValueChangeListener(reference::set);

        component.setValue("");
        Assert.assertNull("no event should be triggered yet", reference.get());

        component.setValue("foobar");

        Assert.assertNotNull("event should have been triggered",
                reference.get());
        HasValue.ValueChangeEvent event = reference.get();

        Assert.assertEquals("invalid previous value", "", event.getOldValue());
        Assert.assertEquals("invalid value", "foobar", event.getValue());

        component.setValue(null);

        event = reference.get();

        Assert.assertEquals("invalid previous value", "foobar",
                event.getOldValue());
        Assert.assertEquals("invalid value", "", event.getValue());

        reference.set(null);

        component.setValue("");
        Assert.assertNull("no event should be triggered yet", reference.get());
    }

}
