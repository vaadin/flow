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

import java.security.InvalidParameterException;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.function.SerializableConsumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PropertyBindingTest {

    PropertyData<Integer> intData = new PropertyData<>("int",
            Integer.class, false, 0);
    PropertyData<Integer> intData_readOnly = new PropertyData<>("int",
            Integer.class, true, 0);
    PropertyData<String> stringData = new PropertyData<>("string",
            String.class, false, "");
    PropertyData<String> stringData_null = new PropertyData<>("string",
            String.class, false, null);



    @Test
    public void updateValue() {

        PropertyBinding<Integer> binding_noListener =
                new PropertyBinding<>(intData, null);

        binding_noListener.updateValue(1);

        assertEquals("Value should have been updated", (Integer) 1,
                binding_noListener.getValue());


        IntConsumer consumer = new IntConsumer();
        SerializableConsumer<Integer> spy = spy(consumer);
        PropertyBinding<Integer> binding_listener =
                new PropertyBinding<>(intData, spy);

        binding_listener.updateValue(2);

        assertEquals("Value should have been updated", (Integer)2,
                binding_listener.getValue());

        // update should have been delivered
        verify(spy, times(1));
    }

    @Test(expected = InvalidParameterException.class)
    public void updateValue_wrongDataType() {
        PropertyBinding<Integer> binding =
                new PropertyBinding<>(intData, null);

        binding.updateValue("cat");
    }

    @Test
    public void addValue_primitiveTypeListener() {
        PrimitiveIntListener listener = new PrimitiveIntListener();

        PropertyBinding<Integer> binding =
                new PropertyBinding<>(intData, listener::setValue);

        binding.updateValue(1);
        Assert.assertEquals(1, listener.value);

        // set to default
        binding.updateValue(null);
        Assert.assertEquals(0, listener.value);
    }

    @Test public void addValue_stringCanBeSetToNullIfDefault() {
        StringListener listener = new StringListener();

        PropertyBinding<String> binding_emptyDefault =
                new PropertyBinding<>(stringData, listener::setValue);

        binding_emptyDefault.updateValue("cat");

        Assert.assertEquals("cat", listener.value);

        binding_emptyDefault.updateValue(null);

        Assert.assertEquals("", listener.value);


        PropertyBinding<String> binding_nullDefault =
                new PropertyBinding<>(stringData_null, listener::setValue);

        binding_nullDefault.updateValue("cat");

        Assert.assertEquals("cat", listener.value);

        binding_nullDefault.updateValue(null);

        Assert.assertNull(listener.value);
    }

    @Test(expected = IllegalStateException.class)
    public void addValue_throwsWhenReadOnly() {
        PropertyBinding<Integer> binding =
                new PropertyBinding<>(intData_readOnly, null);

        binding.updateValue(1);
    }

    @Test
    public void getType() {
        PropertyBinding<Integer> binding =
                new PropertyBinding<>(intData, null);

        Assert.assertEquals(Integer.class, binding.getType());
    }

    @Test
    public void getName() {
        PropertyBinding<Integer> binding =
                new PropertyBinding<>(intData, null);

        Assert.assertEquals("int", binding.getName());
    }

    @Test
    public void isReadOnly() {
        PropertyBinding<Integer> binding =
                new PropertyBinding<>(intData, null);

        Assert.assertFalse(binding.isReadOnly());

        PropertyBinding<Integer> binding_readOnly =
                new PropertyBinding<>(intData_readOnly, null);

        Assert.assertTrue(binding_readOnly.isReadOnly());
    }

    private static class IntConsumer implements SerializableConsumer<Integer> {
        @Override
        public void accept(Integer integer) {

        }
    }

    private static class PrimitiveIntListener {
        int value = 0;

        public void setValue(int v) {
            value = v;
        }
    }

    private static class StringListener {
        String value;

        public void setValue(String s) {
            value = s;
        }
    }
}