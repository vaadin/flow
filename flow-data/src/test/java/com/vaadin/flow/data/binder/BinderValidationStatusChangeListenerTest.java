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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.data.binder.testcomponents.TestHasValidatorDatePicker;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.tests.data.bean.Person;

import static com.vaadin.flow.data.binder.testcomponents.TestHasValidatorDatePicker.INVALID_DATE_FORMAT;

public class BinderValidationStatusChangeListenerTest
        extends BinderTestBase<Binder<Person>, Person> {

    private static final String BIRTH_DATE_PROPERTY = "birthDate";

    private final Map<HasValue<?, ?>, String> componentErrors = new HashMap<>();

    @Before
    public void setUp() {
        binder = new Binder<>(Person.class) {
            @Override
            protected void handleError(HasValue<?, ?> field,
                    ValidationResult result) {
                componentErrors.put(field, result.getErrorMessage());
            }

            @Override
            protected void clearError(HasValue<?, ?> field) {
                super.clearError(field);
                componentErrors.remove(field);
            }
        };
        item = new Person();
    }

    @Test
    public void fieldWithHasValidatorDefaults_bindIsCalled_addValidationStatusListenerIsCalled() {
        var field = Mockito.spy(
                TestHasValidatorDatePicker.DatePickerHasValidatorDefaults.class);
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Mockito.verify(field, Mockito.times(1))
                .addValidationStatusChangeListener(Mockito.any());
    }

    @Test
    public void binderWithFieldsValidationStatusChangeListenerDisabled_bindIsCalled_noValidationStatusListenerIsCalled() {
        binder.setFieldsValidationStatusChangeListenerEnabled(false);
        var field = Mockito.spy(
                TestHasValidatorDatePicker.DatePickerHasValidatorDefaults.class);
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Mockito.verify(field, Mockito.never())
                .addValidationStatusChangeListener(Mockito.any());
    }

    @Test
    public void fieldWithHasValidatorOnlyGetDefaultValidatorOverridden_bindIsCalled_addValidationStatusListenerIsCalled() {
        var field = Mockito.spy(
                TestHasValidatorDatePicker.DataPickerHasValidatorGetDefaultValidatorOverridden.class);
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Mockito.verify(field, Mockito.times(1))
                .addValidationStatusChangeListener(Mockito.any());
    }

    @Test
    public void fieldWithHasValidatorOnlyAddListenerOverridden_bindIsCalled_addValidationStatusListenerIsCalled() {
        var field = Mockito.spy(
                TestHasValidatorDatePicker.DataPickerHasValidatorAddListenerOverridden.class);
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Mockito.verify(field, Mockito.times(1))
                .addValidationStatusChangeListener(Mockito.any());
    }

    @Test
    public void fieldWithHasValidatorFullyOverridden_bindIsCalled_addValidationStatusChangeListenerIsCalled() {
        var field = Mockito.spy(
                TestHasValidatorDatePicker.DataPickerHasValidatorOverridden.class);
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Mockito.verify(field, Mockito.times(1))
                .addValidationStatusChangeListener(Mockito.any());
    }

    @Test
    public void fieldWithHasValidatorFullyOverridden_fieldValidationStatusChangesToFalse_binderHandleErrorIsCalled() {
        var field = new TestHasValidatorDatePicker.DataPickerHasValidatorOverridden();
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Assert.assertEquals(0, componentErrors.size());

        field.fireValidationStatusChangeEvent(false);
        Assert.assertEquals(1, componentErrors.size());
        Assert.assertEquals(INVALID_DATE_FORMAT, componentErrors.get(field));
    }

    @Test
    public void binderWithFieldsValidationStatusChangeListenerDisabled_fieldValidationStatusChangesToFalse_binderHandleErrorIsNotCalled() {
        binder.setFieldsValidationStatusChangeListenerEnabled(false);
        var field = new TestHasValidatorDatePicker.DataPickerHasValidatorOverridden();
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Assert.assertEquals(0, componentErrors.size());

        field.fireValidationStatusChangeEvent(false);
        Assert.assertEquals(0, componentErrors.size());
    }

    @Test
    public void fieldWithHasValidatorFullyOverridden_fieldValidationStatusChangesToTrue_binderClearErrorIsCalled() {
        var field = new TestHasValidatorDatePicker.DataPickerHasValidatorOverridden();
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Assert.assertEquals(0, componentErrors.size());

        field.fireValidationStatusChangeEvent(false);
        Assert.assertEquals(1, componentErrors.size());
        Assert.assertEquals(INVALID_DATE_FORMAT, componentErrors.get(field));

        field.fireValidationStatusChangeEvent(true);
        Assert.assertEquals(0, componentErrors.size());
        Assert.assertNull(componentErrors.get(field));
    }

    @Test
    public void fieldWithHasValidatorOnlyAddListenerOverriddenAndCustomValidation_fieldValidationStatusChangesToFalse_binderHandleErrorIsCalled() {
        var field = new TestHasValidatorDatePicker.DataPickerHasValidatorAddListenerOverridden();
        binder.forField(field).withValidator(field::customValidation)
                .bind(BIRTH_DATE_PROPERTY);

        field.fireValidationStatusChangeEvent(false);
        Assert.assertEquals(1, componentErrors.size());
        Assert.assertEquals(INVALID_DATE_FORMAT, componentErrors.get(field));
    }

    @Test
    public void fieldWithHasValidatorOnlyAddListenerOverriddenAndCustomValidation_fieldValidationStatusChangesToTrue_binderClearErrorIsCalled() {
        var field = new TestHasValidatorDatePicker.DataPickerHasValidatorAddListenerOverridden();
        binder.forField(field).withValidator(field::customValidation)
                .bind(BIRTH_DATE_PROPERTY);

        field.fireValidationStatusChangeEvent(false);
        Assert.assertEquals(1, componentErrors.size());
        Assert.assertEquals(INVALID_DATE_FORMAT, componentErrors.get(field));

        field.fireValidationStatusChangeEvent(true);
        Assert.assertEquals(0, componentErrors.size());
        Assert.assertNull(componentErrors.get(field));
    }

    @Test
    public void fieldWithHasValidatorFullyOverridden_boundFieldGetsUnbind_validationStatusChangeListenerInBindingIsRemoved() {
        TestHasValidatorDatePicker.DataPickerHasValidatorOverridden field = new TestHasValidatorDatePicker.DataPickerHasValidatorOverridden();
        Binder.Binding<Person, LocalDate> binding = binder.bind(field,
                BIRTH_DATE_PROPERTY);
        Assert.assertEquals(0, componentErrors.size());

        field.fireValidationStatusChangeEvent(false);
        Assert.assertEquals(1, componentErrors.size());
        Assert.assertEquals(INVALID_DATE_FORMAT, componentErrors.get(field));

        binding.unbind();

        field.fireValidationStatusChangeEvent(true);
        // after unbind is called, validationStatusChangeListener
        // in the binding is not working anymore, errors are not cleared:
        Assert.assertEquals(1, componentErrors.size());
        Assert.assertEquals(INVALID_DATE_FORMAT, componentErrors.get(field));
    }

    @Test
    public void fieldWithHasValidator_validationStatusChangesToTrueWithNullValue_beanIsUpdated() {
        // Setup: bind field to bean with an initial date value
        var field = new TestHasValidatorDatePicker.DataPickerHasValidatorOverridden();
        LocalDate initialDate = LocalDate.of(2023, 1, 15);
        item.setBirthDate(initialDate);
        binder.bind(field, BIRTH_DATE_PROPERTY);
        binder.setBean(item);

        // Verify initial state
        Assert.assertEquals(initialDate, item.getBirthDate());
        Assert.assertEquals(initialDate, field.getValue());

        // Simulate: user enters invalid input (field keeps null value
        // internally,
        // validation fails)
        // field.setValue(null);
        field.fireValidationStatusChangeEvent(false);

        // Bean should still have old value since validation failed
        Assert.assertEquals(1, componentErrors.size());
        Assert.assertEquals(initialDate, item.getBirthDate());

        // Simulate: user clears the field to null (which is now accepted)
        // Field value is already null, but validation now passes
        field.setValue(null);
        field.fireValidationStatusChangeEvent(true);

        // Error should be cleared
        Assert.assertEquals(0, componentErrors.size());

        // Bug: Bean should be updated to null, but currently it's not
        Assert.assertNull(
                "Bean property should be updated to null when validation passes",
                item.getBirthDate());
    }

}
