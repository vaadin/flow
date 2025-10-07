/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.tests.data.bean.BeanToValidate;

public class UnbindTest
        extends BinderTestBase<Binder<BeanToValidate>, BeanToValidate> {
    @Before
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
        Assert.assertEquals(1, binder.getBindings().size());
        firstnameBinding.unbind();
        Assert.assertTrue(binder.getBindings().isEmpty());
        Assert.assertNull(firstnameBinding.getField());
    }

    @Test
    public void binding_unbindDuringReadBean_shouldBeRemovedFromBindings() {
        Binder.Binding<BeanToValidate, String> firstnameBinding = binder
                .bind(nameField, "firstname");
        Binder.Binding<BeanToValidate, String> ageBinding = binder
                .bind(ageField, "age");
        Assert.assertEquals(2, binder.getBindings().size());
        nameField.addValueChangeListener(event -> {
            if (event.getValue().length() > 0)
                ageBinding.unbind();
        });
        binder.readBean(item);
        Assert.assertEquals(1, binder.getBindings().size());
        Assert.assertNull(ageBinding.getField());
    }

    @Test
    public void binding_unbindTwice_shouldBeRemovedFromBindings() {
        Binder.Binding<BeanToValidate, String> firstnameBinding = binder
                .bind(nameField, "firstname");
        Assert.assertEquals(1, binder.getBindings().size());
        firstnameBinding.unbind();
        firstnameBinding.unbind();
        Assert.assertTrue(binder.getBindings().isEmpty());
        Assert.assertNull(firstnameBinding.getField());
    }
}
