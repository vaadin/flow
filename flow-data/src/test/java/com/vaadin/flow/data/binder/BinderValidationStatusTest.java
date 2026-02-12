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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.BindingValidationStatus.Status;
import com.vaadin.flow.data.binder.testcomponents.TestLabel;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.tests.data.bean.Person;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BinderValidationStatusTest
        extends BinderTestBase<Binder<Person>, Person> {
    private final Map<HasValue<?, ?>, String> componentErrors = new HashMap<>();

    protected final static BindingValidationStatusHandler NOOP = event -> {
    };

    @BeforeEach
    public void setUp() {
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

    //
    // Binding-level status handler
    //

    @Test
    public void bindingWithStatusHandler_handlerGetsEvents() {
        AtomicReference<BindingValidationStatus<?>> statusCapture = new AtomicReference<>();
        BindingBuilder<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty).withValidationStatusHandler(evt -> {
                    Assertions.assertNull(statusCapture.get());
                    statusCapture.set(evt);
                });
        binding.bind(Person::getFirstName, Person::setFirstName);

        nameField.setValue("");

        // First validation fails => should be event with ERROR status and
        // message
        binder.validate();

        Assertions.assertNotNull(statusCapture.get());
        BindingValidationStatus<?> evt = statusCapture.get();
        Assertions.assertEquals(Status.ERROR, evt.getStatus());
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE, evt.getMessage().get());
        Assertions.assertEquals(nameField, evt.getField());

        statusCapture.set(null);
        nameField.setValue("foo");

        statusCapture.set(null);
        // Second validation succeeds => should be event with OK status and
        // no message
        binder.validate();

        evt = statusCapture.get();
        Assertions.assertNotNull(evt);
        Assertions.assertEquals(Status.OK, evt.getStatus());
        Assertions.assertFalse(evt.getMessage().isPresent());
        Assertions.assertEquals(nameField, evt.getField());
    }

    @Test
    public void bindingWithStatusLabel_labelIsUpdatedAccordingStatus() {
        TestLabel label = new TestLabel();

        Binding<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty).withStatusLabel(label)
                .bind(Person::getFirstName, Person::setFirstName);

        nameField.setValue("");

        // First validation fails => should be event with ERROR status and
        // message
        binding.validate();

        assertVisible(label, true);
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE, label.getText());

        nameField.setValue("foo");

        // Second validation succeeds => should be event with OK status and
        // no message
        binding.validate();

        assertVisible(label, false);
        Assertions.assertEquals("", label.getText());
    }

    @Test
    public void bindingWithStatusLabel_fieldInvalidStateIsSet() {
        TestLabel label = new TestLabel();

        Binding<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty).withStatusLabel(label)
                .bind(Person::getFirstName, Person::setFirstName);

        Assertions.assertNull(componentErrors.get(nameField));

        nameField.setValue("");

        // First validation fails => should be event with ERROR status and
        // message
        binding.validate();

        // withStatusLabel updates both the label and the field's invalid state
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE,
                componentErrors.get(nameField));
    }

    @Test
    public void bindingWithStatusHandler_addAfterBound() {
        assertThrows(IllegalStateException.class, () -> {
            BindingBuilder<Person, String> binding = binder.forField(nameField)
                    .withValidator(notEmpty);
            binding.bind(Person::getFirstName, Person::setFirstName);

            binding.withValidationStatusHandler(evt -> Assertions.fail());
        });
    }

    @Test
    public void bindingWithStatusLabel_addAfterBound() {
        assertThrows(IllegalStateException.class, () -> {
            TestLabel label = new TestLabel();

            BindingBuilder<Person, String> binding = binder.forField(nameField)
                    .withValidator(notEmpty);
            binding.bind(Person::getFirstName, Person::setFirstName);

            binding.withStatusLabel(label);
        });
    }

    @Test
    public void bindingWithStatusLabel_setAfterHandler() {
        assertThrows(IllegalStateException.class, () -> {
            TestLabel label = new TestLabel();

            BindingBuilder<Person, String> binding = binder.forField(nameField);

            binding.withValidationStatusHandler(NOOP);

            binding.withStatusLabel(label);
        });
    }

    @Test
    public void bindingWithStatusHandler_setAfterLabel() {
        assertThrows(IllegalStateException.class, () -> {
            TestLabel label = new TestLabel();

            BindingBuilder<Person, String> binding = binder.forField(nameField);

            binding.withStatusLabel(label);

            binding.withValidationStatusHandler(NOOP);
        });
    }

    @Test
    public void bindingWithStatusHandler_setAfterOtherHandler() {
        assertThrows(IllegalStateException.class, () -> {
            BindingBuilder<Person, String> binding = binder.forField(nameField);

            binding.withValidationStatusHandler(NOOP);

            binding.withValidationStatusHandler(NOOP);
        });
    }

    //
    // Binder-level status handler
    //

    @Test
    public void binderWithStatusHandler_fieldValidationNoBeanValidation_handlerGetsStatusUpdates() {
        AtomicReference<BinderValidationStatus<?>> statusCapture = new AtomicReference<>();
        binder.forField(nameField).withValidator(notEmpty)
                .withValidationStatusHandler(evt -> Assertions.fail(
                        "Using a custom status change handler so no change should end up here"))
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField).withConverter(stringToInteger)
                .withValidator(notNegative)
                .withValidationStatusHandler(evt -> Assertions.fail(
                        "Using a custom status change handler so no change should end up here"))
                .bind(Person::getAge, Person::setAge);

        binder.setValidationStatusHandler(statusCapture::set);
        binder.setBean(item);
        Assertions.assertNull(componentErrors.get(nameField));

        nameField.setValue("");
        ageField.setValue("5");

        // First binding validation fails => should be result with ERROR status
        // and message
        BinderValidationStatus<Person> status2 = binder.validate();
        BinderValidationStatus<?> status = statusCapture.get();
        Assertions.assertSame(status2, status);

        Assertions.assertNull(componentErrors.get(nameField));

        List<BindingValidationStatus<?>> bindingStatuses = status
                .getFieldValidationStatuses();
        Assertions.assertNotNull(bindingStatuses);
        Assertions.assertEquals(1, status.getFieldValidationErrors().size());
        Assertions.assertEquals(2, bindingStatuses.size());

        BindingValidationStatus<?> r = bindingStatuses.get(0);
        Assertions.assertTrue(r.isError());
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE, r.getMessage().get());
        Assertions.assertEquals(nameField, r.getField());

        r = bindingStatuses.get(1);
        Assertions.assertFalse(r.isError());
        Assertions.assertFalse(r.getMessage().isPresent());
        Assertions.assertEquals(ageField, r.getField());

        Assertions.assertEquals(0, status.getBeanValidationResults().size());
        Assertions.assertEquals(0, status.getBeanValidationErrors().size());

        nameField.setValue("foo");
        ageField.setValue("");

        statusCapture.set(null);
        // Second validation succeeds => should be result with OK status and
        // no message, and error result for age
        binder.validate();

        status = statusCapture.get();
        bindingStatuses = status.getFieldValidationStatuses();
        Assertions.assertEquals(1, status.getFieldValidationErrors().size());
        Assertions.assertEquals(2, bindingStatuses.size());

        r = bindingStatuses.get(0);
        Assertions.assertFalse(r.isError());
        Assertions.assertFalse(r.getMessage().isPresent());
        Assertions.assertEquals(nameField, r.getField());

        r = bindingStatuses.get(1);
        Assertions.assertTrue(r.isError());
        Assertions.assertEquals(NOT_NUMBER_ERROR_MESSAGE, r.getMessage().get());
        Assertions.assertEquals(ageField, r.getField());

        Assertions.assertEquals(0, status.getBeanValidationResults().size());
        Assertions.assertEquals(0, status.getBeanValidationErrors().size());

        statusCapture.set(null);
        // binding validations pass, binder validation fails
        ageField.setValue("0");
        binder.validate();

        status = statusCapture.get();
        bindingStatuses = status.getFieldValidationStatuses();
        Assertions.assertEquals(0, status.getFieldValidationErrors().size());
        Assertions.assertEquals(2, bindingStatuses.size());

        Assertions.assertEquals(0, status.getBeanValidationResults().size());
        Assertions.assertEquals(0, status.getBeanValidationErrors().size());
    }

    @Test
    public void binderWithStatusHandler_fieldAndBeanLevelValidation_handlerGetsStatusUpdates() {
        AtomicReference<BinderValidationStatus<?>> statusCapture = new AtomicReference<>();
        binder.forField(nameField).withValidator(notEmpty)
                .withValidationStatusHandler(evt -> Assertions.fail(
                        "Using a custom status change handler so no change should end up here"))
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField).withConverter(stringToInteger)
                .withValidator(notNegative)
                .withValidationStatusHandler(evt -> Assertions.fail(
                        "Using a custom status change handler so no change should end up here"))
                .bind(Person::getAge, Person::setAge);
        binder.withValidator(
                bean -> !bean.getFirstName().isEmpty() && bean.getAge() > 0,
                "Need first name and age");

        binder.setValidationStatusHandler(statusCapture::set);
        binder.setBean(item);
        Assertions.assertNull(componentErrors.get(nameField));

        nameField.setValue("");
        ageField.setValue("5");

        // First binding validation fails => should be result with ERROR status
        // and message
        BinderValidationStatus<Person> status2 = binder.validate();
        BinderValidationStatus<?> status = statusCapture.get();
        Assertions.assertSame(status2, status);

        Assertions.assertNull(componentErrors.get(nameField));

        List<BindingValidationStatus<?>> bindingStatuses = status
                .getFieldValidationStatuses();
        Assertions.assertNotNull(bindingStatuses);
        Assertions.assertEquals(1, status.getFieldValidationErrors().size());
        Assertions.assertEquals(2, bindingStatuses.size());

        BindingValidationStatus<?> r = bindingStatuses.get(0);
        Assertions.assertTrue(r.isError());
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE, r.getMessage().get());
        Assertions.assertEquals(nameField, r.getField());

        r = bindingStatuses.get(1);
        Assertions.assertFalse(r.isError());
        Assertions.assertFalse(r.getMessage().isPresent());
        Assertions.assertEquals(ageField, r.getField());

        Assertions.assertEquals(0, status.getBeanValidationResults().size());
        Assertions.assertEquals(0, status.getBeanValidationErrors().size());

        nameField.setValue("foo");
        ageField.setValue("");

        statusCapture.set(null);
        // Second validation succeeds => should be result with OK status and
        // no message, and error result for age
        binder.validate();

        status = statusCapture.get();
        bindingStatuses = status.getFieldValidationStatuses();
        Assertions.assertEquals(1, status.getFieldValidationErrors().size());
        Assertions.assertEquals(2, bindingStatuses.size());

        r = bindingStatuses.get(0);
        Assertions.assertFalse(r.isError());
        Assertions.assertFalse(r.getMessage().isPresent());
        Assertions.assertEquals(nameField, r.getField());

        r = bindingStatuses.get(1);
        Assertions.assertTrue(r.isError());
        Assertions.assertEquals(NOT_NUMBER_ERROR_MESSAGE, r.getMessage().get());
        Assertions.assertEquals(ageField, r.getField());

        Assertions.assertEquals(0, status.getBeanValidationResults().size());
        Assertions.assertEquals(0, status.getBeanValidationErrors().size());

        statusCapture.set(null);
        // binding validations pass, binder validation fails
        ageField.setValue("0");
        binder.validate();

        status = statusCapture.get();
        bindingStatuses = status.getFieldValidationStatuses();
        Assertions.assertEquals(0, status.getFieldValidationErrors().size());
        Assertions.assertEquals(2, bindingStatuses.size());

        Assertions.assertEquals(1, status.getBeanValidationResults().size());
        Assertions.assertEquals(1, status.getBeanValidationErrors().size());

        Assertions.assertEquals("Need first name and age",
                status.getBeanValidationErrors().get(0).getErrorMessage());
    }

    @Test
    public void binderWithStatusHandler_defaultStatusHandlerIsReplaced() {
        Binding<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty).withValidationStatusHandler(evt -> {
                }).bind(Person::getFirstName, Person::setFirstName);

        Assertions.assertNull(componentErrors.get(nameField));

        nameField.setValue("");

        // First validation fails => should be event with ERROR status and
        // message
        binding.validate();

        // no component error since default handler is replaced
        Assertions.assertNull(componentErrors.get(nameField));
    }

    @Test
    public void binderWithStatusHandler_addAfterBound() {
        assertThrows(IllegalStateException.class, () -> {
            BindingBuilder<Person, String> binding = binder.forField(nameField)
                    .withValidator(notEmpty);
            binding.bind(Person::getFirstName, Person::setFirstName);

            binding.withValidationStatusHandler(evt -> Assertions.fail());
        });
    }

    @Test
    public void binderWithStatusLabel_addAfterBound() {
        assertThrows(IllegalStateException.class, () -> {
            TestLabel label = new TestLabel();

            BindingBuilder<Person, String> binding = binder.forField(nameField)
                    .withValidator(notEmpty);
            binding.bind(Person::getFirstName, Person::setFirstName);

            binding.withStatusLabel(label);
        });
    }

    @Test
    public void binderWithStatusLabel_setAfterHandler() {
        assertThrows(IllegalStateException.class, () -> {
            TestLabel label = new TestLabel();

            BindingBuilder<Person, String> binding = binder.forField(nameField);
            binding.bind(Person::getFirstName, Person::setFirstName);

            binder.setValidationStatusHandler(event -> {
            });

            binder.setStatusLabel(label);
        });
    }

    @Test
    public void binderWithStatusHandler_setAfterLabel() {
        assertThrows(IllegalStateException.class, () -> {
            TestLabel label = new TestLabel();

            BindingBuilder<Person, String> binding = binder.forField(nameField);
            binding.bind(Person::getFirstName, Person::setFirstName);

            binder.setStatusLabel(label);

            binder.setValidationStatusHandler(event -> {
            });
        });
    }

    @Test
    public void binderWithNullStatusHandler_throws() {
        assertThrows(NullPointerException.class, () -> {
            binder.setValidationStatusHandler(null);
        });
    }

    @Test
    public void binderWithStatusHandler_replaceHandler() {
        AtomicReference<BinderValidationStatus<?>> capture = new AtomicReference<>();

        BindingBuilder<Person, String> binding = binder.forField(nameField);
        binding.bind(Person::getFirstName, Person::setFirstName);

        binder.setValidationStatusHandler(results -> Assertions.fail());
        binder.setValidationStatusHandler(capture::set);

        nameField.setValue("foo");
        binder.validate();

        List<BindingValidationStatus<?>> results = capture.get()
                .getFieldValidationStatuses();
        Assertions.assertNotNull(results);
        Assertions.assertEquals(1, results.size());
        Assertions.assertFalse(results.get(0).isError());
        assertValidField(nameField);
    }

    private void assertVisible(TestLabel label, boolean visible) {
        if (visible) {
            Assertions.assertNull(label.getStyle().get("display"));
        } else {
            Assertions.assertEquals("none", label.getStyle().get("display"));
        }
    }

    // Tests for issue #21707: withStatusLabel should set field invalid state
    @Test
    public void withStatusLabel_validationError_setsFieldInvalidAndUpdatesLabel() {
        TestLabel label = new TestLabel();

        binder.forField(nameField).withValidator(notEmpty)
                .withStatusLabel(label)
                .bind(Person::getFirstName, Person::setFirstName);

        nameField.setValue("");
        binder.validate();

        // Both label and field invalid state should be updated
        assertVisible(label, true);
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE, label.getText());
        Assertions.assertTrue(nameField.isInvalid());
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE,
                componentErrors.get(nameField));
    }

    @Test
    public void withStatusLabel_validValue_clearsFieldInvalidAndLabel() {
        TestLabel label = new TestLabel();

        binder.forField(nameField).withValidator(notEmpty)
                .withStatusLabel(label)
                .bind(Person::getFirstName, Person::setFirstName);

        // First set invalid
        nameField.setValue("");
        binder.validate();
        Assertions.assertTrue(nameField.isInvalid());

        // Then set valid
        nameField.setValue("Valid");
        binder.validate();

        // Both label and field invalid state should be cleared
        assertVisible(label, false);
        Assertions.assertEquals("", label.getText());
        Assertions.assertFalse(nameField.isInvalid());
        Assertions.assertNull(componentErrors.get(nameField));
    }

    @Test
    public void withStatusLabel_customValidator_setsFieldInvalid() {
        TestLabel label = new TestLabel();
        String customError = "Must start with uppercase";

        binder.forField(nameField)
                .withValidator(value -> Character.isUpperCase(value.charAt(0)),
                        customError)
                .withStatusLabel(label)
                .bind(Person::getFirstName, Person::setFirstName);

        nameField.setValue("lowercase");
        binder.validate();

        // Field should be marked invalid with custom validator
        Assertions.assertTrue(nameField.isInvalid());
        Assertions.assertEquals(customError, componentErrors.get(nameField));
        assertVisible(label, true);
        Assertions.assertEquals(customError, label.getText());
    }

    @Test
    public void withStatusLabel_requiredField_setsFieldInvalid() {
        TestLabel label = new TestLabel();

        binder.forField(nameField).asRequired(EMPTY_ERROR_MESSAGE)
                .withStatusLabel(label)
                .bind(Person::getFirstName, Person::setFirstName);

        nameField.setValue("");
        binder.validate();

        // Required field validation should set field invalid
        Assertions.assertTrue(nameField.isInvalid());
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE,
                componentErrors.get(nameField));
        assertVisible(label, true);
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE, label.getText());
    }

    @Test
    public void withStatusLabel_conversionError_setsFieldInvalid() {
        TestLabel label = new TestLabel();
        TestTextField ageField = new TestTextField();
        String conversionError = "Must be a number";

        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(conversionError))
                .withStatusLabel(label).bind(Person::getAge, Person::setAge);

        ageField.setValue("not a number");
        binder.validate();

        // Conversion error should set field invalid
        Assertions.assertTrue(ageField.isInvalid());
        assertVisible(label, true);
        Assertions.assertEquals(conversionError, label.getText());
    }

    @Test
    public void withStatusLabel_multipleValidators_setsFieldInvalid() {
        TestLabel label = new TestLabel();
        String lengthError = "Must be at least 3 characters";

        binder.forField(nameField).withValidator(notEmpty)
                .withValidator(value -> value.length() >= 3, lengthError)
                .withStatusLabel(label)
                .bind(Person::getFirstName, Person::setFirstName);

        nameField.setValue("ab");
        binder.validate();

        // Multiple validators should still set field invalid
        Assertions.assertTrue(nameField.isInvalid());
        Assertions.assertEquals(lengthError, componentErrors.get(nameField));
        assertVisible(label, true);
        Assertions.assertEquals(lengthError, label.getText());
    }

    @Test
    public void withValidationStatusHandler_customHandler_fieldInvalidNotSetAutomatically() {
        AtomicReference<BindingValidationStatus<?>> statusCapture = new AtomicReference<>();

        binder.forField(nameField).withValidator(notEmpty)
                .withValidationStatusHandler(statusCapture::set)
                .bind(Person::getFirstName, Person::setFirstName);

        nameField.setValue("");
        binder.validate();

        // Custom handler gives full control - field invalid state should NOT
        // be set automatically
        Assertions.assertFalse(nameField.isInvalid());
        Assertions.assertNull(componentErrors.get(nameField));

        // But the handler should still receive the validation status
        Assertions.assertNotNull(statusCapture.get());
        Assertions.assertEquals(Status.ERROR, statusCapture.get().getStatus());
        Assertions.assertEquals(EMPTY_ERROR_MESSAGE,
                statusCapture.get().getMessage().get());
    }

    @Test
    public void withValidationStatusHandler_customHandler_canManuallySetFieldInvalid() {
        binder.forField(nameField).withValidator(notEmpty)
                .withValidationStatusHandler(status -> {
                    // Custom handler that manually controls field invalid state
                    if (status.isError()) {
                        nameField.setInvalid(true);
                    } else {
                        nameField.setInvalid(false);
                    }
                }).bind(Person::getFirstName, Person::setFirstName);

        nameField.setValue("");
        binder.validate();

        // Custom handler manually set the invalid state
        Assertions.assertTrue(nameField.isInvalid());
        // But componentErrors should still be null because we're using custom
        // handler
        Assertions.assertNull(componentErrors.get(nameField));

        // Clear the error
        nameField.setValue("Valid");
        binder.validate();

        // Custom handler should clear the invalid state
        Assertions.assertFalse(nameField.isInvalid());
    }

    @Test
    public void binderValidationStatus_nullBindingStatuses() {
        try {
            new BinderValidationStatus<>(new Binder<Person>(), null,
                    Collections.emptyList());
            Assertions.fail("Binder should throw an NPE");
        } catch (NullPointerException npe) {
            Assertions.assertNotNull(npe.getMessage());
        }
    }
}
