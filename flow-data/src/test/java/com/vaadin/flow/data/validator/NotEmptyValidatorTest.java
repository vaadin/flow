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
package com.vaadin.flow.data.validator;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
class NotEmptyValidatorTest {

    @Test
    void nullValueIsDisallowed() {
        NotEmptyValidator<String> validator = new NotEmptyValidator<>("foo");
        ValidationResult result = validator.apply(null,
                new ValueContext(new Binder()));
        assertTrue(result.isError());
        assertEquals("foo", result.getErrorMessage());
    }

    @Test
    void emptyValueIsDisallowed() {
        NotEmptyValidator<String> validator = new NotEmptyValidator<>("foo");
        ValidationResult result = validator.apply("",
                new ValueContext(new Binder()));
        assertTrue(result.isError());
        assertEquals("foo", result.getErrorMessage());
    }

    @Test
    void nonNullValueIsAllowed() {
        NotEmptyValidator<Object> validator = new NotEmptyValidator<>("foo");
        Object value = new Object();
        ValidationResult result = validator.apply(value,
                new ValueContext(new Binder()));
        assertFalse(result.isError());
        assertFalse(result.isError());
    }
}
