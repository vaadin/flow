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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.WritableSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.tests.data.bean.Person;

/**
 * Tests for Binder and Binding integration with Signals.
 */
class BinderSignalTest extends SignalsUnitTest {

    private TestTextField firstNameField;
    private TestTextField lastNameField;
    private Binder<Person> binder;
    private Person item;

    @BeforeEach
    public void setup() {
        binder = new Binder<>(Person.class);
        item = new Person();
        firstNameField = new TestTextField();
        lastNameField = new TestTextField();
    }

    /**
     * Returns validator that is valid only if both target value and cross-field
     * binding value are not empty. Calls Binding.value() to get the other field
     * value.
     */
    private SerializablePredicate<String> hasTextValuesValidator(
            Binder.Binding<?, String> otherFieldBinding) {
        return (String value) -> !value.isEmpty()
                && !otherFieldBinding.value().isEmpty();
    }

    // verifies that Binding.value() works with property name bindings
    @Test
    public void bindingValue_withBinderBindPropertyName() {
        item.setFirstName("Alice");

        var field = new TestTextField();
        var binding = binder.bind(field, "firstName");

        Assertions.assertEquals("", binding.value());
        Assertions.assertEquals("", field.getValue());

        binder.setBean(item);

        Assertions.assertEquals("Alice", binding.value());
        Assertions.assertEquals("Alice", field.getValue());
    }

    // verifies that Binding.value() works with getter/setter bindings
    @Test
    public void bindingValue_withBinderBindGetterSetter() {
        binder = new Binder<>();
        item.setFirstName("Alice");

        var field = new TestTextField();
        var binding = binder.bind(field, Person::getFirstName,
                Person::setFirstName);

        Assertions.assertEquals("", binding.value());
        Assertions.assertEquals("", field.getValue());

        binder.setBean(item);

        Assertions.assertEquals("Alice", binding.value());
        Assertions.assertEquals("Alice", field.getValue());
    }

    // verifies that Binding.value() with a signal-bound field works correctly
    @Test
    public void bindingValue_withSignal() {
        binder = new Binder<>();
        item.setFirstName("Alice");

        var signal = new ValueSignal<>("");

        var field = new TestTextField();
        field.bindValue(signal);

        var binding = binder.bind(field, Person::getFirstName,
                Person::setFirstName);
        binder.setBean(item);
        signal.value("foo");

        Assertions.assertEquals("Alice", binding.value());

        UI.getCurrent().add(field);
        Assertions.assertEquals("foo", binding.value());

        signal.value("bar");
        Assertions.assertEquals("bar", binding.value());

        field.bindValue(null);
        signal.value("baz");
        Assertions.assertEquals("bar", binding.value());
    }

    // verifies that cross-field validation works with signal-bound fields
    @Test
    public void bindingValue_crossFieldValidation_withSignal() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        var firstNameSignal = new ValueSignal<>("");
        var lastNameSignal = new ValueSignal<>("");

        firstNameField.bindValue(firstNameSignal);
        lastNameField.bindValue(lastNameSignal);

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");
        binder.setBean(item);

        Assertions.assertTrue(binder.isValid());

        // change of last name triggers validation of first name binding
        lastNameSignal.value("");

        Assertions.assertTrue(firstNameField.isInvalid());
        Assertions.assertEquals("First and last name are required", binder
                .validate().getValidationErrors().getFirst().getErrorMessage());
    }

    // verifies that cross-field validation works also without signal-bound
    // fields
    @Test
    public void bindingValue_crossFieldValidation_withoutSignal() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");
        binder.setBean(item);

        Assertions.assertTrue(binder.isValid());

        // change of last name triggers validation of first name binding
        lastNameField.setValue("");

        Assertions.assertTrue(firstNameField.isInvalid());
        Assertions.assertEquals("First and last name are required", binder
                .validate().getValidationErrors().getFirst().getErrorMessage());
    }

    // verifies that cross-field validation works with mix of signal-bound and
    // not-bound fields
    @Test
    public void bindingValue_crossFieldValidation_withMixedFields() {
        item.setFirstName("Alice");
        item.setLastName("Smith");
        item.setAge(30);
        item.setEmail("email");

        var firstNameSignal = new ValueSignal<>("");
        var lastNameSignal = new ValueSignal<>("");
        var ageSignal = new ValueSignal<>("0");

        firstNameField.bindValue(firstNameSignal);
        lastNameField.bindValue(lastNameSignal);
        var ageField = new TestTextField();
        ageField.bindValue(ageSignal);

        var emailField = new TestTextField();

        UI.getCurrent().add(firstNameField, lastNameField, emailField,
                ageField);

        binder.forField(firstNameField).bind("firstName");
        var lastNameBinding = binder.forField(lastNameField).bind("lastName");
        var emailBinding = binder.forField(emailField).bind("email");

        String ageValidationError = "First name, last name and age are required";
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(
                        "Value must be an integer"))
                .withValidator(
                        value -> value > 0 && !lastNameBinding.value().isEmpty()
                                && !firstNameSignal.value().isEmpty()
                                && !emailBinding.value().isEmpty(),
                        ageValidationError)
                .bind("age");
        binder.setBean(item);

        Assertions.assertTrue(binder.isValid());

        // change of any field triggers validation of age field binding
        emailField.setValue("");

        Assertions.assertTrue(ageField.isInvalid());
        Assertions.assertEquals(ageValidationError, ageField.getErrorMessage());

        // rest of the test changes fields one by one to verify that age field
        // validation is re-triggered each time
        emailField.setValue("email");
        Assertions.assertFalse(ageField.isInvalid());

        firstNameField.setValue("");
        Assertions.assertTrue(ageField.isInvalid());
        Assertions.assertEquals(ageValidationError, ageField.getErrorMessage());

        firstNameField.setValue("John");
        lastNameField.setValue("");
        Assertions.assertTrue(ageField.isInvalid());
        Assertions.assertEquals(ageValidationError, ageField.getErrorMessage());

        lastNameField.setValue("Smith");
        ageField.setValue("0");
        Assertions.assertTrue(ageField.isInvalid());
        Assertions.assertEquals(ageValidationError, ageField.getErrorMessage());

        ageField.setValue("10");
        Assertions.assertFalse(ageField.isInvalid());
        Assertions.assertEquals(0,
                binder.validate().getValidationErrors().size());
    }

    // verifies that cross-field validation works correctly with setBean
    @Test
    public void crossFieldValidation_setBean_validationTriggeredOnFieldChange() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        // Initially no bean is set, fields should be empty
        Assertions.assertEquals("", firstNameField.getValue());
        Assertions.assertEquals("", lastNameField.getValue());

        // Set bean - fields should be populated
        binder.setBean(item);

        Assertions.assertEquals("Alice", firstNameField.getValue());
        Assertions.assertEquals("Smith", lastNameField.getValue());
        Assertions.assertTrue(binder.isValid());

        // Change last name to empty - should trigger validation of first name
        lastNameField.setValue("");

        Assertions.assertTrue(firstNameField.isInvalid());
        Assertions.assertEquals("First and last name are required",
                firstNameField.getErrorMessage());

        // Bean should be updated immediately with setBean
        Assertions.assertEquals("", item.getLastName());
    }

    // verifies that cross-field validation works correctly with readBean
    @Test
    public void crossFieldValidation_readBean_validationTriggeredOnFieldChange() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        // Initially no bean is set, fields should be empty
        Assertions.assertEquals("", firstNameField.getValue());
        Assertions.assertEquals("", lastNameField.getValue());

        // Read bean - fields should be populated
        binder.readBean(item);

        Assertions.assertEquals("Alice", firstNameField.getValue());
        Assertions.assertEquals("Smith", lastNameField.getValue());
        Assertions.assertTrue(binder.isValid());

        // Change last name to empty - should trigger validation of first name
        lastNameField.setValue("");

        Assertions.assertTrue(firstNameField.isInvalid());
        Assertions.assertEquals("First and last name are required",
                firstNameField.getErrorMessage());

        // Bean should NOT be updated with readBean
        Assertions.assertEquals("Smith", item.getLastName());
    }

    // verifies that cross-field validation works correctly with readBean and
    // signal-bound fields
    @Test
    public void crossFieldValidation_readBean_withSignals() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        var firstNameSignal = new ValueSignal<>("");
        var lastNameSignal = new ValueSignal<>("");

        firstNameField.bindValue(firstNameSignal);
        lastNameField.bindValue(lastNameSignal);

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        binder.readBean(item);

        Assertions.assertEquals("Alice", firstNameField.getValue());
        Assertions.assertEquals("Smith", lastNameField.getValue());
        Assertions.assertTrue(binder.isValid());

        // Change last name via signal
        lastNameSignal.value("");

        Assertions.assertTrue(firstNameField.isInvalid());
        Assertions.assertEquals("First and last name are required",
                firstNameField.getErrorMessage());

        // Bean should NOT be updated with readBean
        Assertions.assertEquals("Smith", item.getLastName());
    }

    // verifies that switching from setBean to readBean works correctly
    @Test
    public void crossFieldValidation_switchFromSetBeanToReadBean() {
        var item1 = new Person();
        item1.setFirstName("Alice");
        item1.setLastName("Smith");

        var item2 = new Person();
        item2.setFirstName("Bob");
        item2.setLastName("Jones");

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        // Start with setBean
        binder.setBean(item1);
        Assertions.assertEquals("Alice", firstNameField.getValue());
        Assertions.assertEquals("Smith", lastNameField.getValue());

        // Modify fields - should update item1
        firstNameField.setValue("Charlie");
        Assertions.assertEquals("Charlie", item1.getFirstName());

        // Switch to readBean with item2
        binder.readBean(item2);
        Assertions.assertEquals("Bob", firstNameField.getValue());
        Assertions.assertEquals("Jones", lastNameField.getValue());

        // Modify fields - should NOT update item2
        firstNameField.setValue("David");
        Assertions.assertEquals("Bob", item2.getFirstName());

        // Cross-field validation should still work
        lastNameField.setValue("");
        Assertions.assertTrue(firstNameField.isInvalid());
    }

    // verifies that cross-field validation works with records
    @Test
    public void crossFieldValidation_record_readRecord() {
        record TestRecord(String firstName, String lastName) {
        }

        var binder = new Binder<>(TestRecord.class);
        var record = new TestRecord("Alice", "Smith");

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        // Read record - fields should be populated
        binder.readRecord(record);

        Assertions.assertEquals("Alice", firstNameField.getValue());
        Assertions.assertEquals("Smith", lastNameField.getValue());
        Assertions.assertTrue(binder.isValid());

        // Change last name to empty - should trigger validation of first name
        lastNameField.setValue("");

        Assertions.assertTrue(firstNameField.isInvalid());
        Assertions.assertEquals("First and last name are required",
                firstNameField.getErrorMessage());
    }

    // verifies that cross-field validation works with records and signal-bound
    // fields
    @Test
    public void crossFieldValidation_record_readRecord_withSignals() {
        record TestRecord(String firstName, String lastName) {
        }

        var binder = new Binder<>(TestRecord.class);
        var record = new TestRecord("Alice", "Smith");

        var firstNameSignal = new ValueSignal<>("");
        var lastNameSignal = new ValueSignal<>("");

        firstNameField.bindValue(firstNameSignal);
        lastNameField.bindValue(lastNameSignal);

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        binder.readRecord(record);

        Assertions.assertEquals("Alice", firstNameField.getValue());
        Assertions.assertEquals("Smith", lastNameField.getValue());
        Assertions.assertTrue(binder.isValid());

        // Change last name via signal
        lastNameSignal.value("");

        Assertions.assertTrue(firstNameField.isInvalid());
        Assertions.assertEquals("First and last name are required",
                firstNameField.getErrorMessage());
    }

    // verifies that multiple bean changes work correctly with cross-field
    // validation
    @Test
    public void crossFieldValidation_multipleBeanChanges() {
        var item1 = new Person();
        item1.setFirstName("Alice");
        item1.setLastName("Smith");

        var item2 = new Person();
        item2.setFirstName("Bob");
        item2.setLastName("");

        var item3 = new Person();
        item3.setFirstName("Charlie");
        item3.setLastName("Brown");

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        // Set first bean - should be valid
        binder.setBean(item1);
        Assertions.assertTrue(binder.isValid());
        Assertions.assertFalse(firstNameField.isInvalid());

        // Set second bean with empty last name - should be invalid
        binder.setBean(item2);
        Assertions.assertEquals("Bob", firstNameField.getValue());
        Assertions.assertEquals("", lastNameField.getValue());
        Assertions.assertFalse(binder.isValid());

        // Validate to trigger error display
        binder.validate();
        Assertions.assertTrue(firstNameField.isInvalid());

        // Set third bean - should be valid again
        binder.setBean(item3);
        Assertions.assertEquals("Charlie", firstNameField.getValue());
        Assertions.assertEquals("Brown", lastNameField.getValue());
        Assertions.assertTrue(binder.isValid());
    }

    // verifies that cross-field validation state is preserved when changing
    // beans
    @Test
    public void crossFieldValidation_validationStatePreservedOnBeanChange() {
        var item1 = new Person();
        item1.setFirstName("Alice");
        item1.setLastName("Smith");

        var item2 = new Person();
        item2.setFirstName("");
        item2.setLastName("");

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        // Set first bean and make it invalid
        binder.setBean(item1);
        lastNameField.setValue("");
        Assertions.assertTrue(firstNameField.isInvalid());

        // Set second bean - validation state should be cleared
        binder.setBean(item2);
        Assertions.assertEquals("", firstNameField.getValue());
        Assertions.assertEquals("", lastNameField.getValue());

        // Validate the new bean
        binder.validate();
        Assertions.assertTrue(firstNameField.isInvalid());
        Assertions.assertEquals("First and last name are required",
                firstNameField.getErrorMessage());
    }

    // verifies that cross-field validation with signals only works when fields
    // are attached
    @Test
    public void crossFieldValidation_onlyWorksWithAttachedFields() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        var firstNameSignal = new ValueSignal<>("");
        var lastNameSignal = new ValueSignal<>("");

        firstNameField.bindValue(firstNameSignal);
        lastNameField.bindValue(lastNameSignal);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        binder.setBean(item);

        // Fields are NOT attached yet
        Assertions.assertTrue(binder.isValid());

        // Change last name via signal - should NOT trigger cross-field
        // validation
        // because fields are not attached
        lastNameSignal.value("");

        // First name field should still be valid because the signal change
        // doesn't trigger cross-field validation when detached
        Assertions.assertFalse(firstNameField.isInvalid());

        // Change last name field directly - should NOT trigger cross-field
        // validation because fields are not attached and ComponentEffect is not
        // active for Binder's internal trigger signal.
        lastNameField.setValue("");

        Assertions.assertFalse(firstNameField.isInvalid());
        Assertions.assertEquals("", firstNameField.getErrorMessage());

        // Reset to valid state
        lastNameField.setValue("Smith");
        binder.validate();
        Assertions.assertFalse(firstNameField.isInvalid());
        Assertions.assertTrue(binder.isValid());

        // Now attach the fields
        UI.getCurrent().add(firstNameField, lastNameField);

        // Change last name via signal - NOW it should trigger cross-field
        // validation
        lastNameSignal.value("");

        // First name field should be invalid because cross-field validation
        // is triggered automatically when fields are attached
        Assertions.assertTrue(firstNameField.isInvalid());
        Assertions.assertEquals("First and last name are required",
                firstNameField.getErrorMessage());
    }

    // verifies that unbind() removes signal registration.
    @Test
    public void unbind_removesSignalRegistration() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        var firstNameSignal = new ValueSignal<>("");
        var lastNameSignal = new ValueSignal<>("");

        firstNameField.bindValue(firstNameSignal);
        lastNameField.bindValue(lastNameSignal);

        UI.getCurrent().add(firstNameField, lastNameField);

        var firstNameBinding = binder.forField(firstNameField)
                .bind("firstName");
        binder.forField(lastNameField)
                .withValidator(
                        value -> !value.isEmpty()
                                && !firstNameBinding.value().isEmpty(),
                        "Both names required")
                .bind("lastName");

        binder.setBean(item);

        // Change firstName via signal - should trigger cross-field validation
        firstNameSignal.value("");
        Assertions.assertTrue(lastNameField.isInvalid());

        // Reset to valid state
        firstNameSignal.value("Alice");
        binder.validate();
        Assertions.assertFalse(lastNameField.isInvalid());

        // Unbind the firstName binding
        firstNameBinding.unbind();

        // Fields stay attached.
        // After unbind, signal registration should be removed
        // Change firstName via signal - should NOT trigger cross-field
        // validation
        firstNameSignal.value("");
        Assertions.assertFalse(lastNameField.isInvalid());
    }

    @Test
    public void beanLevelValidator_throwWhenSignalIsUsed() {
        item.setFirstName("Alice");
        var firstNameSignal = new ValueSignal<>("");
        UI.getCurrent().add(firstNameField);
        binder.forField(firstNameField).bind("firstName");
        binder.setBean(item);

        binder.withValidator(bean -> {
            firstNameSignal.peek(); // ok
            return true;
        }, "Bean level validation failed");

        Assertions.assertTrue(binder.isValid());

        binder.withValidator(bean -> {
            firstNameSignal.value(); // causes error
            return true;
        }, "Bean level validation with a signal failed");

        Assertions.assertThrows(Binder.InvalidSignalUsageError.class,
                () -> binder.validate());
        Assertions.assertThrows(Binder.InvalidSignalUsageError.class,
                () -> binder.isValid());
    }

    @Test
    public void getValidationStatus_signalInitialized() {
        Signal<BinderValidationStatus<Person>> statusSignal = binder
                .getValidationStatus();
        Assertions.assertNotNull(statusSignal,
                "getValidationStatus() should setup validation status signal");
        Assertions.assertNotNull(statusSignal.value(),
                "validation status signal value should not be null initially");
    }

    @Test
    public void getValidationStatus_signalIsReadOnly() {
        Signal<BinderValidationStatus<Person>> statusSignal = binder
                .getValidationStatus();
        Assertions.assertThrows(ClassCastException.class,
                () -> ((WritableSignal<BinderValidationStatus<Person>>) statusSignal)
                        .value(null));
    }

    @Test
    public void getValidationStatus_statusChangeUpdatesSignal() {
        item.setFirstName("Alice");
        item.setLastName("Smith");
        UI.getCurrent().add(firstNameField, lastNameField);
        binder.forField(firstNameField)
                .withValidator(value -> !value.isEmpty(), "").bind("firstName");
        binder.forField(lastNameField)
                .withValidator(value -> !value.isEmpty(), "").bind("lastName");
        binder.setBean(item);

        Assertions.assertTrue(binder.getValidationStatus().value().isOk());

        firstNameField.setValue("");
        Assertions.assertFalse(binder.getValidationStatus().value().isOk());
        firstNameField.setValue("foo");
        Assertions.assertTrue(binder.getValidationStatus().value().isOk());
        lastNameField.setValue("");
        Assertions.assertFalse(binder.getValidationStatus().value().isOk());
        firstNameField.setValue("");
        Assertions.assertFalse(binder.getValidationStatus().value().isOk());
        firstNameField.setValue("foo");
        Assertions.assertFalse(binder.getValidationStatus().value().isOk());
    }

    // verifies that field-specific validation statuses are updated correctly
    @Test
    public void getValidationStatus_fieldChanged_validationStatusSignalUpdated() {
        item.setFirstName("Alice");
        item.setLastName("Smith");
        UI.getCurrent().add(firstNameField, lastNameField);
        var lastNameBinding = binder.forField(lastNameField).bind("lastName");
        var firstNameBinding = binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");
        binder.setBean(item);

        Assertions.assertTrue(binder.getValidationStatus().value()
                .getFieldValidationStatuses().stream()
                .noneMatch(BindingValidationStatus::isError));
        Assertions.assertTrue(binder.getValidationStatus().value().isOk());

        lastNameField.setValue(""); // change to invalid state

        Assertions.assertFalse(binder.getValidationStatus().value().isOk());
        var firstNameValidationStatuses = binder.getValidationStatus().value()
                .getFieldValidationStatuses().stream()
                .filter(status -> status.getBinding() == firstNameBinding)
                .toList();
        var otherValidationStatuses = binder.getValidationStatus().value()
                .getFieldValidationStatuses().stream()
                .filter(status -> status.getBinding() != firstNameBinding)
                .toList();
        Assertions.assertEquals(1, firstNameValidationStatuses.size(),
                "Expected one BindingValidationStatus for first name field");
        Assertions.assertEquals(1, otherValidationStatuses.size(),
                "Expected one BindingValidationStatus for last name field");
        Assertions.assertFalse(otherValidationStatuses.get(0).isError(),
                "Expected last name field to NOT have an error");
        Assertions.assertTrue(firstNameValidationStatuses.get(0).isError(),
                "Expected first name field to have an error");

        lastNameField.setValue("Smith");
        Assertions.assertTrue(binder.getValidationStatus().value().isOk());
    }

    @Test
    public void bindingValue_converterNotTracking() {
        item.setLastName("Smith");
        item.setAge(30);

        var ageField = new TestTextField();

        UI.getCurrent().add(lastNameField, ageField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        AtomicInteger converterCalls = new AtomicInteger(0);
        String ageValidationError = "Last name and age are required";
        binder.forField(ageField).withConverter(
                new StringToIntegerConverter("Value must be an integer") {
                    @Override
                    public Result<Integer> convertToModel(String value,
                            ValueContext context) {
                        // this should not start tracking
                        lastNameBinding.value();
                        converterCalls.incrementAndGet();
                        return super.convertToModel(value, context);
                    }
                })
                .withValidator(
                        value -> value > 0
                                && !lastNameField.getValue().isEmpty(),
                        ageValidationError)
                .bind("age");
        binder.setBean(item);

        Assertions.assertTrue(binder.isValid());

        // change of last name doesn't trigger validation of age field binding
        // because converter is run inside UsageTracker.untracked()
        lastNameField.setValue("");

        Assertions.assertFalse(ageField.isInvalid());
        Assertions.assertEquals("", ageField.getErrorMessage());
        Assertions.assertTrue(converterCalls.get() > 0,
                "Converter should be called at least once");
        Assertions.assertFalse(binder.isValid()); // reruns all validators
    }

    @Test
    public void getValidationStatus_setBean_statusChangeRunEffects() {
        testStatusChangeRunEffects(() -> binder.setBean(item));
    }

    @Test
    public void getValidationStatus_readBean_statusChangeRunEffects() {
        testStatusChangeRunEffects(() -> binder.readBean(item));
    }

    private void testStatusChangeRunEffects(Runnable binderSetup) {
        item.setFirstName("Alice");
        item.setLastName("Smith");
        UI.getCurrent().add(firstNameField, lastNameField);
        binder.forField(firstNameField)
                .withValidator(value -> !value.isEmpty(), "").bind("firstName");
        binder.forField(lastNameField)
                .withValidator(value -> !value.isEmpty(), "").bind("lastName");
        binderSetup.run();

        AtomicInteger effectCalled = new AtomicInteger(0);
        AtomicBoolean prevStatus = new AtomicBoolean(true);
        ComponentEffect.effect(firstNameField, () -> {
            prevStatus.set(binder.getValidationStatus().value().isOk());
            effectCalled.incrementAndGet();
        });

        // change field values to valid and invalid back-and-forth
        firstNameField.setValue("");
        Assertions.assertFalse(prevStatus.get());
        Assertions.assertEquals(2, effectCalled.get());

        firstNameField.setValue("foo");
        Assertions.assertEquals(3, effectCalled.get());
        Assertions.assertTrue(prevStatus.get());

        lastNameField.setValue("");
        Assertions.assertEquals(4, effectCalled.get());
        Assertions.assertFalse(prevStatus.get());

        firstNameField.setValue("");
        Assertions.assertFalse(prevStatus.get());
        Assertions.assertEquals(5, effectCalled.get());
        firstNameField.setValue("foo");
        Assertions.assertEquals(6, effectCalled.get());
        Assertions.assertFalse(prevStatus.get(),
                "Binder status change signal should be invalid when other field is still invalid.");
    }

    @Test
    public void getValidationStatus_setBean_initialStatus() {
        testInitialStatusChangeRunEffects(item -> binder.setBean(item));
    }

    @Test
    public void getValidationStatus_readBean_initialStatus() {
        testInitialStatusChangeRunEffects(item -> binder.readBean(item));
    }

    private void testInitialStatusChangeRunEffects(
            Consumer<Person> binderSetup) {
        item.setFirstName("");
        UI.getCurrent().add(firstNameField);
        binder.forField(firstNameField)
                .withValidator(value -> !value.isEmpty(), "").bind("firstName");
        binderSetup.accept(item);

        AtomicBoolean prevStatus = new AtomicBoolean(true);
        ComponentEffect.effect(firstNameField, () -> {
            prevStatus.set(binder.getValidationStatus().value().isOk());
        });

        Assertions.assertFalse(prevStatus.get());

        firstNameField.setValue("foo");
        Assertions.assertTrue(prevStatus.get());

        var person = new Person();
        person.setFirstName("");

        binderSetup.accept(person);
        Assertions.assertFalse(prevStatus.get());
    }
}
