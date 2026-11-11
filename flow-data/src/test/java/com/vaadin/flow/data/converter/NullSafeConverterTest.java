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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.binder.testcomponents.TestDatePicker;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullSafeConverterTest {

    record ValueObject(String value) {
        ValueObject {
            requireNonNull(value);
        }
    }

    private final Converter<String, ValueObject> wrapped = Converter
            .from(ValueObject::new, ValueObject::value, Exception::getMessage);

    @Test
    void nullSafeConverter_mapsNullRepresentationToNullAndBack() {
        var converter = Converter.nullSafeConverter(wrapped, "-");
        var context = new ValueContext();

        assertNull(convertToModel(converter, "-"));
        assertNull(convertToModel(converter, null));
        assertEquals(new ValueObject("hello"),
                convertToModel(converter, "hello"));
        assertEquals("-", converter.convertToPresentation(null, context));
        assertEquals("hello", converter
                .convertToPresentation(new ValueObject("hello"), context));

        var showsNothing = Converter.nullSafeConverter(
                Converter.<String, ValueObject> from(ValueObject::new,
                        value -> null, Exception::getMessage),
                "-");
        assertEquals("-", showsNothing
                .convertToPresentation(new ValueObject("hello"), context));
    }

    @Test
    void binder_datePickerWithoutNullRepresentation() {
        var field = new TestDatePicker();
        field.setValue(LocalDate.of(2003, 7, 14));
        var binder = new Binder<Bean<Instant>>();
        binder.forField(field)
                .withConverter(Converter.nullSafeConverter(Converter.from(
                        date -> date.atStartOfDay(ZoneOffset.UTC).toInstant(),
                        instant -> LocalDate.ofInstant(instant, ZoneOffset.UTC),
                        Exception::getMessage)))
                .bind(Bean::getValue, Bean::setValue);
        var bean = new Bean<Instant>();
        binder.setBean(bean);
        assertNull(field.getValue());

        field.setValue(LocalDate.of(2011, 3, 9));
        assertEquals(Instant.parse("2011-03-09T00:00:00Z"), bean.getValue());

        field.clear();
        assertTrue(binder.isValid());
        assertNull(bean.getValue());
    }

    private static ValueObject convertToModel(
            Converter<String, ValueObject> converter, String value) {
        return converter.convertToModel(value, new ValueContext())
                .getOrThrow(IllegalStateException::new);
    }

    public static class Bean<T> {
        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }
}
