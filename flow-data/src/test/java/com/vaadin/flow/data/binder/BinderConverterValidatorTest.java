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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.testcomponents.TestLabel;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.NotEmptyValidator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.tests.data.bean.Person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinderConverterValidatorTest
        extends BinderTestBase<Binder<Person>, Person> {

    private Map<HasValue<?, ?>, String> componentErrors = new HashMap<>();

    private static class StatusBean implements Serializable {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

    }

    @BeforeEach
    public void setUp() {
        CurrentInstance.clearAll();
        binder = new Binder<Person>() {
            @Override
            protected void handleError(HasValue<?, ?> field,
                    ValidationResult result) {
                super.handleError(field, result);
                componentErrors.put(field, result.getErrorMessage());
            }

            @Override
            protected void clearError(HasValue<?, ?> field) {
                super.clearError(field);
                componentErrors.remove(field);
            }
        };
        item = new Person();
        item.setFirstName("Johannes");
        item.setAge(32);
    }

    @Test
    public void validate_notBound_noErrors() {
        BinderValidationStatus<Person> status = binder.validate();

        assertTrue(status.isOk());
    }

    @Test
    public void bound_validatorsAreOK_noErrors() {
        BindingBuilder<Person, String> binding = binder.forField(nameField);
        binding.withValidator(Validator.alwaysPass()).bind(Person::getFirstName,
                Person::setFirstName);

        BinderValidationStatus<Person> status = binder.validate();

        assertTrue(status.isOk());
        assertFalse(componentErrors.containsKey(nameField));
        assertValidField(nameField);
    }

    @Test
    public void bound_validatorsFail_errors() {
        BindingBuilder<Person, String> binding = binder.forField(nameField);
        binding.withValidator(Validator.alwaysPass());
        String errorMessage = "foo";
        binding.withValidator((String value,
                ValueContext context) -> ValidationResult.error(errorMessage));
        binding.withValidator(value -> false, "bar");
        binding.bind(Person::getFirstName, Person::setFirstName);

        BinderValidationStatus<Person> status = binder.validate();
        List<BindingValidationStatus<?>> errors = status
                .getFieldValidationErrors();

        assertEquals(1, errors.size());

        BindingValidationStatus<?> validationStatus = errors.stream()
                .findFirst().get();
        assertEquals(errorMessage, validationStatus.getMessage().get());

        HasValue<?, ?> field = validationStatus.getField();

        assertEquals(nameField, field);

        assertEquals(errorMessage, componentErrors.get(nameField));
        assertInvalidField(errorMessage, nameField);
    }

    @Test
    public void validatorForSuperTypeCanBeUsed() {
        // Validates that a validator for a super type can be used, e.g.
        // validator for Number can be used on a Double

        TestTextField salaryField = new TestTextField();
        Validator<Number> positiveNumberValidator = (value, context) -> {
            if (value.doubleValue() >= 0) {
                return ValidationResult.ok();
            } else {
                return ValidationResult.error(NEGATIVE_ERROR_MESSAGE);
            }
        };
        binder.forField(salaryField)
                .withConverter(Double::valueOf, String::valueOf)
                .withValidator(positiveNumberValidator)
                .bind(Person::getSalaryDouble, Person::setSalaryDouble);

        Person person = new Person();
        binder.setBean(person);
        salaryField.setValue("10");
        assertEquals(10, person.getSalaryDouble(), 0);
        salaryField.setValue("-1"); // Does not pass validator
        assertEquals(10, person.getSalaryDouble(), 0);
        assertInvalidField(NEGATIVE_ERROR_MESSAGE, salaryField);
    }

    @Test
    public void convertInitialValue() {
        bindAgeWithValidatorConverterValidator();
        assertEquals("32", ageField.getValue());
    }

    @Test
    public void convertToModelValidAge() {
        bindAgeWithValidatorConverterValidator();

        ageField.setValue("33");
        assertEquals(33, item.getAge());
    }

    @Test
    public void convertToModelNegativeAgeFailsOnFirstValidator() {
        bindAgeWithValidatorConverterValidator();

        ageField.setValue("");
        assertEquals(32, item.getAge());
        assertValidationError(binder.validate(), EMPTY_ERROR_MESSAGE);
    }

    private void assertValidationError(BinderValidationStatus<Person> status,
            String errorMessage) {
        List<BindingValidationStatus<?>> validationErrors = status
                .getFieldValidationErrors();
        assertEquals(1, validationErrors.size(),
                "Got one error message, should have one validation error");
        BindingValidationStatus<?> error = validationErrors.get(0);
        assertEquals(errorMessage, error.getMessage().get());
        assertInvalidField(errorMessage, (HasValidation) error.getField());
    }

    @Test
    public void convertToModelConversionFails() {
        bindAgeWithValidatorConverterValidator();
        ageField.setValue("abc");
        assertEquals(32, item.getAge());
        assertValidationError(binder.validate(), NOT_NUMBER_ERROR_MESSAGE);
    }

    @Test
    public void convertToModelNegativeAgeFailsOnIntegerValidator() {
        bindAgeWithValidatorConverterValidator();

        ageField.setValue("-5");
        assertEquals(32, item.getAge());
        assertValidationError(binder.validate(), NEGATIVE_ERROR_MESSAGE);
    }

    @Test
    public void convertDataToField() {
        bindAgeWithValidatorConverterValidator();
        binder.getBean().setAge(12);
        binder.readBean(binder.getBean());
        assertEquals("12", ageField.getValue());
        assertValidField(ageField);
    }

    @Test
    public void convertNotValidatableDataToField() {
        bindAgeWithValidatorConverterValidator();
        binder.getBean().setAge(-12);
        binder.readBean(binder.getBean());
        assertEquals("-12", ageField.getValue());
    }

    @Test
    public void convertInvalidDataToField() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestTextField field = new TestTextField();
            StatusBean bean = new StatusBean();
            bean.setStatus("1");
            Binder<StatusBean> binder = new Binder<>();

            BindingBuilder<StatusBean, String> binding = binder.forField(field)
                    .withConverter(presentation -> {
                        if (presentation.equals("OK")) {
                            return "1";
                        } else if (presentation.equals("NOTOK")) {
                            return "2";
                        }
                        throw new IllegalArgumentException(
                                "Value must be OK or NOTOK");
                    }, model -> {
                        if (model.equals("1")) {
                            return "OK";
                        } else if (model.equals("2")) {
                            return "NOTOK";
                        } else {
                            throw new IllegalArgumentException(
                                    "Value in model must be 1 or 2");
                        }
                    });
            binding.bind(StatusBean::getStatus, StatusBean::setStatus);
            binder.setBean(bean);

            bean.setStatus("3");
            binder.readBean(bean);
        });
    }

    @Test
    public void validate_failedBeanValidatorWithoutFieldValidators() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        String msg = "foo";
        binder.withValidator(Validator.from(bean -> false, msg));
        Person person = new Person();
        binder.setBean(person);

        List<BindingValidationStatus<?>> errors = binder.validate()
                .getFieldValidationErrors();
        assertEquals(0, errors.size());
        assertValidField(nameField);
    }

    @Test
    public void validate_failedBeanValidatorWithFieldValidator() {
        String msg = "foo";

        BindingBuilder<Person, String> binding = binder.forField(nameField)
                .withValidator(new NotEmptyValidator<>(msg));
        binding.bind(Person::getFirstName, Person::setFirstName);

        binder.withValidator(Validator.from(bean -> false, msg));
        Person person = new Person();
        binder.setBean(person);

        List<BindingValidationStatus<?>> errors = binder.validate()
                .getFieldValidationErrors();
        assertEquals(1, errors.size());
        BindingValidationStatus<?> error = errors.get(0);
        assertEquals(msg, error.getMessage().get());
        assertEquals(nameField, error.getField());
        assertInvalidField(msg, nameField);
    }

    @Test
    public void validate_failedBothBeanValidatorAndFieldValidator() {
        String msg1 = "foo";

        BindingBuilder<Person, String> binding = binder.forField(nameField)
                .withValidator(new NotEmptyValidator<>(msg1));
        binding.bind(Person::getFirstName, Person::setFirstName);

        String msg2 = "bar";
        binder.withValidator(Validator.from(bean -> false, msg2));
        Person person = new Person();
        binder.setBean(person);

        List<BindingValidationStatus<?>> errors = binder.validate()
                .getFieldValidationErrors();
        assertEquals(1, errors.size());

        BindingValidationStatus<?> error = errors.get(0);

        assertEquals(msg1, error.getMessage().get());
        assertEquals(nameField, error.getField());
        assertEquals(msg1, nameField.getErrorMessage());
        Assertions.assertTrue(nameField.isInvalid());
    }

    @Test
    public void validate_okBeanValidatorWithoutFieldValidators() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        String msg = "foo";
        binder.withValidator(Validator.from(bean -> true, msg));
        Person person = new Person();
        binder.setBean(person);

        assertFalse(binder.validate().hasErrors());
        assertTrue(binder.validate().isOk());
        assertValidField(nameField);
    }

    @Test
    public void binder_saveIfValid() {
        String msg1 = "foo";
        BindingBuilder<Person, String> binding = binder.forField(nameField)
                .withValidator(new NotEmptyValidator<>(msg1));
        binding.bind(Person::getFirstName, Person::setFirstName);

        String beanValidatorErrorMessage = "bar";
        binder.withValidator(
                Validator.from(bean -> false, beanValidatorErrorMessage));
        Person person = new Person();
        String firstName = "first name";
        person.setFirstName(firstName);
        binder.readBean(person);

        nameField.setValue("");
        assertFalse(binder.writeBeanIfValid(person));
        // check that field level-validation failed and bean is not updated
        assertEquals(firstName, person.getFirstName());
        assertInvalidField(msg1, nameField);

        nameField.setValue("new name");

        assertFalse(binder.writeBeanIfValid(person));
        // Bean is updated but reverted
        assertEquals(firstName, person.getFirstName());
        assertValidField(nameField);
    }

    @Test
    public void updateBoundField_bindingValdationFails_beanLevelValidationIsNotRun() {
        bindAgeWithValidatorConverterValidator();
        bindName();

        AtomicBoolean beanLevelValidationRun = new AtomicBoolean();
        binder.withValidator(Validator
                .from(bean -> beanLevelValidationRun.getAndSet(true), ""));

        ageField.setValue("not a number");

        assertFalse(beanLevelValidationRun.get());

        nameField.setValue("foo");
        assertFalse(beanLevelValidationRun.get());
    }

    @Test
    public void updateBoundField_bindingValdationSuccess_beanLevelValidationIsRun() {
        bindAgeWithValidatorConverterValidator();
        bindName();

        AtomicBoolean beanLevelValidationRun = new AtomicBoolean();
        binder.withValidator(Validator
                .from(bean -> beanLevelValidationRun.getAndSet(true), ""));

        ageField.setValue(String.valueOf(12));

        assertTrue(beanLevelValidationRun.get());
        assertValidField(ageField);
    }

    @Test
    public void binderHasChanges() throws ValidationException {
        binder.forField(nameField)
                .withValidator(Validator.from(name -> !"".equals(name),
                        "Name can't be empty"))
                .bind(Person::getFirstName, Person::setFirstName);
        assertFalse(binder.hasChanges());
        binder.setBean(item);
        assertFalse(binder.hasChanges());

        // Bound binder + valid user changes: hasChanges == false
        nameField.setValue("foo");
        assertFalse(binder.hasChanges());

        nameField.setValue("bar");
        binder.writeBeanIfValid(new Person());
        assertFalse(binder.hasChanges());

        // Bound binder + invalid user changes: hasChanges() == true
        nameField.setValue("");
        binder.writeBeanIfValid(new Person());
        assertTrue(binder.hasChanges());

        // Read bean resets hasChanges
        binder.readBean(item);
        assertFalse(binder.hasChanges());

        // Removing a bound bean resets hasChanges
        nameField.setValue("");
        assertTrue(binder.hasChanges());
        binder.removeBean();
        assertFalse(binder.hasChanges());

        // Unbound binder + valid user changes: hasChanges() == true
        nameField.setValue("foo");
        assertTrue(binder.hasChanges());

        // successful writeBean resets hasChanges to false
        binder.writeBeanIfValid(new Person());
        assertFalse(binder.hasChanges());

        // Unbound binder + invalid user changes: hasChanges() == true
        nameField.setValue("");
        assertTrue(binder.hasChanges());

        // unsuccessful writeBean doesn't affect hasChanges
        nameField.setValue("");
        binder.writeBeanIfValid(new Person());
        assertTrue(binder.hasChanges());
    }

    @Test
    public void save_fieldValidationErrors() throws ValidationException {
        assertThrows(ValidationException.class, () -> {
            Binder<Person> binder = new Binder<>();
            String msg = "foo";
            binder.forField(nameField)
                    .withValidator(new NotEmptyValidator<>(msg))
                    .bind(Person::getFirstName, Person::setFirstName);

            Person person = new Person();
            String firstName = "foo";
            person.setFirstName(firstName);
            nameField.setValue("");
            try {
                binder.writeBean(person);
            } finally {
                // Bean should not have been updated
                Assertions.assertEquals(firstName, person.getFirstName());
            }
        });
    }

    @Test
    public void save_beanValidationErrors() {
        Binder<Person> binder = new Binder<>();
        binder.forField(nameField).withValidator(new NotEmptyValidator<>("a"))
                .bind(Person::getFirstName, Person::setFirstName);

        binder.withValidator(Validator.alwaysFail("b"));

        Person person = new Person();
        nameField.setValue("foo");
        try {
            binder.writeBean(person);
            Assertions.fail(
                    "Validation should have failed but it passed instead");
        } catch (ValidationException ex) {
            // writeBean() should run bean validations
            Assertions.assertEquals(1, ex.getBeanValidationErrors().size());
            // field validations pass
            Assertions.assertEquals(0, ex.getFieldValidationErrors().size());
        }
        // Bean should have been updated for item validation but reverted
        assertNull(person.getFirstName());
    }

    @Test
    public void save_fieldsAndBeanLevelValidation() throws ValidationException {
        binder.forField(nameField).withValidator(new NotEmptyValidator<>("a"))
                .bind(Person::getFirstName, Person::setFirstName);

        binder.withValidator(
                Validator.from(person -> person.getLastName() != null, "b"));

        Person person = new Person();
        person.setLastName("bar");
        nameField.setValue("foo");
        binder.writeBean(person);
        Assertions.assertEquals(nameField.getValue(), person.getFirstName());
        Assertions.assertEquals("bar", person.getLastName());
    }

    @Test
    public void saveIfValid_fieldValidationErrors() {
        String msg = "foo";
        binder.forField(nameField).withValidator(new NotEmptyValidator<>(msg))
                .bind(Person::getFirstName, Person::setFirstName);

        Person person = new Person();
        person.setFirstName("foo");
        nameField.setValue("");
        Assertions.assertFalse(binder.writeBeanIfValid(person));
        Assertions.assertEquals("foo", person.getFirstName());
    }

    @Test
    public void saveIfValid_noValidationErrors() {
        String msg = "foo";
        binder.forField(nameField).withValidator(new NotEmptyValidator<>(msg))
                .bind(Person::getFirstName, Person::setFirstName);

        Person person = new Person();
        person.setFirstName("foo");
        nameField.setValue("bar");

        Assertions.assertTrue(binder.writeBeanIfValid(person));
        Assertions.assertEquals("bar", person.getFirstName());
    }

    @Test
    public void saveIfValid_beanValidationErrors() {
        Binder<Person> binder = new Binder<>();
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);

        String msg = "foo";
        binder.withValidator(Validator.from(
                prsn -> prsn.getAddress() != null || prsn.getEmail() != null,
                msg));

        Person person = new Person();
        person.setFirstName("foo");
        nameField.setValue("");
        Assertions.assertFalse(binder.writeBeanIfValid(person));

        Assertions.assertEquals("foo", person.getFirstName());
    }

    @Test
    public void save_null_beanIsUpdated() throws ValidationException {
        Binder<Person> binder = new Binder<>();
        binder.forField(nameField).withConverter(fieldValue -> {
            if ("null".equals(fieldValue)) {
                return null;
            } else {
                return fieldValue;
            }
        }, model -> {
            return model;
        }).bind(Person::getFirstName, Person::setFirstName);

        Person person = new Person();
        person.setFirstName("foo");

        nameField.setValue("null");

        binder.writeBean(person);

        assertNull(person.getFirstName());
    }

    @Test
    public void save_validationErrors_exceptionContainsErrors()
            throws ValidationException {
        String msg = "foo";
        BindingBuilder<Person, String> nameBinding = binder.forField(nameField)
                .withValidator(new NotEmptyValidator<>(msg));
        nameBinding.bind(Person::getFirstName, Person::setFirstName);

        BindingBuilder<Person, Integer> ageBinding = binder.forField(ageField)
                .withConverter(stringToInteger).withValidator(notNegative);
        ageBinding.bind(Person::getAge, Person::setAge);

        Person person = new Person();
        nameField.setValue("");
        ageField.setValue("-1");
        try {
            binder.writeBean(person);
            Assertions.fail();
        } catch (ValidationException exception) {
            List<BindingValidationStatus<?>> validationErrors = exception
                    .getFieldValidationErrors();
            Assertions.assertEquals(2, validationErrors.size());
            BindingValidationStatus<?> error = validationErrors.get(0);
            Assertions.assertEquals(nameField, error.getField());
            Assertions.assertEquals(msg, error.getMessage().get());

            error = validationErrors.get(1);
            Assertions.assertEquals(ageField, error.getField());
            Assertions.assertEquals(NEGATIVE_ERROR_MESSAGE,
                    error.getMessage().get());
        }
    }

    @Test
    public void binderBindAndLoad_clearsErrors() {
        BindingBuilder<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty);
        binding.bind(Person::getFirstName, Person::setFirstName);
        binder.withValidator(bean -> !bean.getFirstName().contains("error"),
                "error");
        Person person = new Person();
        person.setFirstName("");
        binder.setBean(person);

        // initial value is invalid but no error
        Assertions.assertFalse(componentErrors.containsKey(nameField));

        // make error show
        nameField.setValue("foo");
        nameField.setValue("");
        Assertions.assertTrue(componentErrors.containsKey(nameField));

        // bind to another person to see that error is cleared
        person = new Person();
        person.setFirstName("");
        binder.setBean(person);
        // error has been cleared
        Assertions.assertFalse(componentErrors.containsKey(nameField));

        // make show error
        nameField.setValue("foo");
        nameField.setValue("");
        Assertions.assertTrue(componentErrors.containsKey(nameField));

        // load should also clear error
        binder.readBean(person);
        Assertions.assertFalse(componentErrors.containsKey(nameField));

        // bind a new field that has invalid value in bean
        TestTextField lastNameField = new TestTextField();

        // The test starts with a valid value as the last name of the person,
        // since the binder assumes any non-changed values to be valid.
        person.setLastName("bar");

        BindingBuilder<Person, String> binding2 = binder.forField(lastNameField)
                .withValidator(notEmpty);
        binding2.bind(Person::getLastName, Person::setLastName);

        // should not have error shown when initialized
        assertEquals("", lastNameField.getErrorMessage());
        Assertions.assertFalse(lastNameField.isInvalid());

        // Set a value that breaks the validation
        lastNameField.setValue("");
        assertNotNull(lastNameField.getErrorMessage());
        Assertions.assertTrue(lastNameField.isInvalid());

        // add status label to show bean level error
        TestLabel statusLabel = new TestLabel();
        binder.setStatusLabel(statusLabel);
        nameField.setValue("error");

        // no error shown yet because second field validation doesn't pass
        Assertions.assertEquals("", statusLabel.getText());

        // make second field validation pass to get bean validation error
        lastNameField.setValue("foo");
        Assertions.assertEquals("error", statusLabel.getText());

        // reload bean to clear error
        binder.readBean(person);
        Assertions.assertEquals("", statusLabel.getText());

        // reset() should clear all errors and status label
        nameField.setValue("");
        lastNameField.setValue("");
        Assertions.assertTrue(componentErrors.containsKey(nameField));
        Assertions.assertTrue(componentErrors.containsKey(lastNameField));

        binder.removeBean();
        Assertions.assertFalse(componentErrors.containsKey(nameField));
        Assertions.assertFalse(componentErrors.containsKey(lastNameField));
        Assertions.assertEquals("", statusLabel.getText());
    }

    @Test
    public void binderLoad_withCrossFieldValidation_clearsErrors() {
        TestTextField lastNameField = new TestTextField();
        final SerializablePredicate<String> lengthPredicate = v -> v
                .length() > 2;

        BindingBuilder<Person, String> firstNameBinding = binder
                .forField(nameField).withValidator(lengthPredicate, "length");
        firstNameBinding.bind(Person::getFirstName, Person::setFirstName);

        Binding<Person, String> lastNameBinding = binder.forField(lastNameField)
                .withValidator(v -> !nameField.getValue().isEmpty()
                        || lengthPredicate.test(v), "err")
                .withValidator(lengthPredicate, "length")
                .bind(Person::getLastName, Person::setLastName);

        // this will be triggered as a new bean is bound with binder.bind(),
        // causing a validation error to be visible until reset is done
        nameField.addValueChangeListener(v -> lastNameBinding.validate());

        Person person = new Person();
        binder.setBean(person);

        Assertions.assertFalse(componentErrors.containsKey(nameField));
        Assertions.assertFalse(componentErrors.containsKey(lastNameField));

        nameField.setValue("x");

        Assertions.assertTrue(componentErrors.containsKey(nameField));
        Assertions.assertTrue(componentErrors.containsKey(lastNameField));

        binder.setBean(person);

        Assertions.assertFalse(componentErrors.containsKey(nameField));
        Assertions.assertFalse(componentErrors.containsKey(lastNameField));
    }

    protected void bindName() {
        binder.bind(nameField, Person::getFirstName, Person::setFirstName);
        binder.setBean(item);
    }

    protected void bindAgeWithValidatorConverterValidator() {
        binder.forField(ageField).withValidator(notEmpty)
                .withConverter(stringToInteger).withValidator(notNegative)
                .bind(Person::getAge, Person::setAge);
        binder.setBean(item);
    }

    @Test
    public void save_beanValidationErrorsWithConverter()
            throws ValidationException {
        assertThrows(ValidationException.class, () -> {
            Binder<Person> binder = new Binder<>();
            binder.forField(ageField)
                    .withConverter(
                            new StringToIntegerConverter("Can't convert"))
                    .bind(Person::getAge, Person::setAge);

            binder.withValidator(Validator.from(person -> false, "b"));

            Person person = new Person();
            ageField.setValue("1");
            try {
                binder.writeBean(person);
            } finally {
                // Bean should have been updated for item validation but
                // reverted
                Assertions.assertEquals(0, person.getAge());
            }
        });
    }
}
