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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.tests.data.bean.BeanToValidate;

class UnbindTest
        extends BinderTestBase<Binder<BeanToValidate>, BeanToValidate> {
    @BeforeEach
    public void setUp() {
        binder = new BeanValidationBinder<>(BeanToValidate.class);
        item = new BeanToValidate();
        item.setFirstname("Johannes");
        item.setAge(32);
    }

    @Test
    public void binding_unbind_shouldBeRemovedFromBindings() {
        Binder.Binding<BeanToValidate, String> firstnameBinding = binder
                .bind(nameField, "firstname");
        Assertions.assertEquals(1, binder.getBindings().size());
        firstnameBinding.unbind();
        Assertions.assertTrue(binder.getBindings().isEmpty());
        Assertions.assertNull(firstnameBinding.getField());
    }

    @Test
    public void binding_unbindDuringReadBean_shouldBeRemovedFromBindings() {
        Binder.Binding<BeanToValidate, String> firstnameBinding = binder
                .bind(nameField, "firstname");
        Binder.Binding<BeanToValidate, String> ageBinding = binder
                .bind(ageField, "age");
        Assertions.assertEquals(2, binder.getBindings().size());
        nameField.addValueChangeListener(event -> {
            if (event.getValue().length() > 0)
                ageBinding.unbind();
        });
        binder.readBean(item);
        Assertions.assertEquals(1, binder.getBindings().size());
        Assertions.assertNull(ageBinding.getField());
    }

    @Test
    public void binding_unbindTwice_shouldBeRemovedFromBindings() {
        Binder.Binding<BeanToValidate, String> firstnameBinding = binder
                .bind(nameField, "firstname");
        Assertions.assertEquals(1, binder.getBindings().size());
        firstnameBinding.unbind();
        firstnameBinding.unbind();
        Assertions.assertTrue(binder.getBindings().isEmpty());
        Assertions.assertNull(firstnameBinding.getField());
    }
}
