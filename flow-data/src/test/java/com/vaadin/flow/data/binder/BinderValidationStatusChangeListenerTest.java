package com.vaadin.flow.data.binder;

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
    public void fieldWithHasValidatorDefaults_bindIsCalled_addValidationStatusListenerIsNotCalled() {
        var field = Mockito.spy(
                TestHasValidatorDatePicker.DatePickerHasValidatorDefaults.class);
        Assert.assertEquals(Validator.alwaysPass(),
                field.getDefaultValidator());
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Mockito.verify(field, Mockito.times(0))
                .addValidationStatusChangeListener(Mockito.any());
    }

    @Test
    public void fieldWithHasValidatorOnlyAddListenerOverridden_bindIsCalled_addValidationStatusListenerIsNotCalled() {
        var field = Mockito.spy(
                TestHasValidatorDatePicker.DataPickerHasValidatorAddListenerOverridden.class);
        Assert.assertEquals(Validator.alwaysPass(),
                field.getDefaultValidator());
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Mockito.verify(field, Mockito.times(0))
                .addValidationStatusChangeListener(Mockito.any());
    }

    @Test
    public void fieldWithHasValidatorFullyOverridden_bindIsCalled_binderAddsValidationStatusChangeListenerToField() {
        var field = Mockito.spy(
                TestHasValidatorDatePicker.DataPickerHasValidatorOverridden.class);
        Assert.assertNotEquals(Validator.alwaysPass(),
                field.getDefaultValidator());
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
    public void fieldWithHasValidatorFullyOverridden_fieldValidationStatusChangesToTrue_binderClearErrorIsCalled() {
        var field = new TestHasValidatorDatePicker.DataPickerHasValidatorOverridden();
        binder.bind(field, BIRTH_DATE_PROPERTY);
        Assert.assertEquals(0, componentErrors.size());

        field.fireValidationStatusChangeEvent(false);
        Assert.assertEquals(1, componentErrors.size());
        Assert.assertEquals(INVALID_DATE_FORMAT, componentErrors.get(field));

        field.fireValidationStatusChangeEvent(true);
        Assert.assertEquals(0, componentErrors.size());
    }

}
