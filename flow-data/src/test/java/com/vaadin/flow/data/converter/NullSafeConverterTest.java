/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.data.converter;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullSafeConverterTest {

    record ValueObject(String value) {
        ValueObject {
            requireNonNull(value);
        }
    }

    private final Converter<String, ValueObject> converter = Converter
            .nullSafeConverter(Converter.from(ValueObject::new,
                    ValueObject::value, Exception::getMessage));

    @Test
    void nullPresentationConvertsToNullModel() {
        var result = converter.convertToModel(null, new ValueContext());
        assertFalse(result.isError());
        assertNull(result.getOrThrow(RuntimeException::new));
    }

    @Test
    void nonNullPresentationConvertsToNonNullModel() {
        var result = converter.convertToModel("hello", new ValueContext());
        assertFalse(result.isError());
        assertEquals(new ValueObject("hello"),
                result.getOrThrow(RuntimeException::new));
    }

    @Test
    void emptyFieldValueConvertsToNullModel() {
        var field = new TestTextField();
        var result = converter.convertToModel("",
                new ValueContext(field, field));
        assertFalse(result.isError());
        assertNull(result.getOrThrow(RuntimeException::new));
    }

    @Test
    void emptyValueWithoutFieldIsPassedToWrappedConverter() {
        var result = converter.convertToModel("", new ValueContext());
        assertFalse(result.isError());
        assertEquals(new ValueObject(""),
                result.getOrThrow(RuntimeException::new));
    }

    @Test
    void binder_emptyTextField_nullModelValue() {
        var field = new TestTextField();
        var binder = new Binder<Bean>();
        binder.forField(field).withConverter(converter).bind(Bean::getValue,
                Bean::setValue);
        var bean = new Bean();
        bean.setValue(new ValueObject("hello"));
        binder.setBean(bean);
        assertEquals("hello", field.getValue());

        field.setValue("world");
        assertEquals(new ValueObject("world"), bean.getValue());

        field.setValue("");
        assertTrue(binder.isValid());
        assertNull(bean.getValue());
    }

    @Test
    void nullModelConvertsToNullPresentation() {
        var result = converter.convertToPresentation(null, new ValueContext());
        assertNull(result);
    }

    @Test
    void nonNullModelConvertsToNonNullPresentation() {
        var result = converter.convertToPresentation(new ValueObject("hello"),
                new ValueContext());
        assertEquals("hello", result);
    }

    public static class Bean {
        private ValueObject value;

        public ValueObject getValue() {
            return value;
        }

        public void setValue(ValueObject value) {
            this.value = value;
        }
    }
}
