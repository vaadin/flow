package com.vaadin.flow.data.binder.testcomponents;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.shared.Registration;

public class TestHasValidatorDatePicker {

    public static final String INVALID_DATE_FORMAT = "Invalid date format";

    public static class DatePickerHasValidatorDefaults extends TestDatePicker
            implements HasValidator<LocalDate> {

        protected boolean validationStatus = true;
    }

    public static class DataPickerHasValidatorGetDefaultValidatorOverridden
            extends DatePickerHasValidatorDefaults {

        @Override
        public Validator<LocalDate> getDefaultValidator() {
            return (value, context) -> validationStatus ? ValidationResult.ok()
                    : ValidationResult.error(INVALID_DATE_FORMAT);
        }
    }

    public static class DataPickerHasValidatorAddListenerOverridden
            extends DatePickerHasValidatorDefaults {

        private final Collection<ValidationStatusChangeListener<LocalDate>> validationStatusListeners = new ArrayList<>();

        @Override
        public Registration addValidationStatusChangeListener(
                ValidationStatusChangeListener<LocalDate> listener) {
            validationStatusListeners.add(listener);
            return () -> validationStatusListeners.remove(listener);
        }

        public void fireValidationStatusChangeEvent(
                boolean newValidationStatus) {
            if (validationStatus != newValidationStatus) {
                validationStatus = newValidationStatus;
                var event = new ValidationStatusChangeEvent<>(this,
                        newValidationStatus);
                validationStatusListeners.forEach(
                        listener -> listener.validationStatusChanged(event));
            }
        }

        public ValidationResult customValidation(LocalDate value,
                ValueContext context) {
            return validationStatus ? ValidationResult.ok()
                    : ValidationResult.error(INVALID_DATE_FORMAT);
        }
    }

    public static class DataPickerHasValidatorOverridden
            extends DataPickerHasValidatorAddListenerOverridden {

        @Override
        public Validator<LocalDate> getDefaultValidator() {
            return (value, context) -> validationStatus ? ValidationResult.ok()
                    : ValidationResult.error("Invalid date format");
        }
    }
}
