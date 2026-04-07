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
package com.vaadin.flow.data.binder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.tests.data.bean.BeanToValidate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnbindTest
        extends BinderTestBase<Binder<BeanToValidate>, BeanToValidate> {
    @BeforeEach
    void setUp() {
        binder = new BeanValidationBinder<>(BeanToValidate.class);
        item = new BeanToValidate();
        item.setFirstname("Johannes");
        item.setAge(32);
    }

    @Test
    void binding_unbind_shouldBeRemovedFromBindings() {
        Binder.Binding<BeanToValidate, String> firstnameBinding = binder
                .bind(nameField, "firstname");
        assertEquals(1, binder.getBindings().size());
        firstnameBinding.unbind();
        assertTrue(binder.getBindings().isEmpty());
        assertNull(firstnameBinding.getField());
    }

    @Test
    void binding_unbindDuringReadBean_shouldBeRemovedFromBindings() {
        Binder.Binding<BeanToValidate, String> firstnameBinding = binder
                .bind(nameField, "firstname");
        Binder.Binding<BeanToValidate, String> ageBinding = binder
                .bind(ageField, "age");
        assertEquals(2, binder.getBindings().size());
        nameField.addValueChangeListener(event -> {
            if (event.getValue().length() > 0)
                ageBinding.unbind();
        });
        binder.readBean(item);
        assertEquals(1, binder.getBindings().size());
        assertNull(ageBinding.getField());
    }

    @Test
    void binding_unbindTwice_shouldBeRemovedFromBindings() {
        Binder.Binding<BeanToValidate, String> firstnameBinding = binder
                .bind(nameField, "firstname");
        assertEquals(1, binder.getBindings().size());
        firstnameBinding.unbind();
        firstnameBinding.unbind();
        assertTrue(binder.getBindings().isEmpty());
        assertNull(firstnameBinding.getField());
    }
}
