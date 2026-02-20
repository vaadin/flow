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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.impl.UsageTracker;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.tests.data.bean.Person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Binder and Binding integration with Signals.
 */
public class BinderSignalTest extends SignalsUnitTest {

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

        assertEquals("", binding.value());
        assertEquals("", field.getValue());

        binder.setBean(item);

        assertEquals("Alice", binding.value());
        assertEquals("Alice", field.getValue());
    }

    // verifies that Binding.value() works with getter/setter bindings
    @Test
    public void bindingValue_withBinderBindGetterSetter() {
        binder = new Binder<>();
        item.setFirstName("Alice");

        var field = new TestTextField();
        var binding = binder.bind(field, Person::getFirstName,
                Person::setFirstName);

        assertEquals("", binding.value());
        assertEquals("", field.getValue());

        binder.setBean(item);

        assertEquals("Alice", binding.value());
        assertEquals("Alice", field.getValue());
    }

    // verifies that Binding.value() with a signal-bound field works correctly
    @Test
    public void bindingValue_withSignal() {
        binder = new Binder<>();
        item.setFirstName("Alice");

        var signal = new ValueSignal<>("");

        var field = new TestTextField();
        field.bindValue(signal, signal::set);

        var binding = binder.bind(field, Person::getFirstName,
                Person::setFirstName);
        binder.setBean(item);
        signal.set("foo");

        assertEquals("Alice", binding.value());

        UI.getCurrent().add(field);
        assertEquals("foo", binding.value());

        signal.set("bar");
        assertEquals("bar", binding.value());
    }

    // verifies that bindValue throws NPE for null signal
    @Test
    public void bindValue_nullSignal_throwsNPE() {
        var field = new TestTextField();
        assertThrows(NullPointerException.class,
                () -> field.bindValue(null, null));
    }

    // verifies that cross-field validation works with signal-bound fields
    @Test
    public void bindingValue_crossFieldValidation_withSignal() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        var firstNameSignal = new ValueSignal<>("");
        var lastNameSignal = new ValueSignal<>("");

        firstNameField.bindValue(firstNameSignal, firstNameSignal::set);
        lastNameField.bindValue(lastNameSignal, lastNameSignal::set);

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");
        binder.setBean(item);

        assertTrue(binder.isValid());

        // change of last name triggers validation of first name binding
        lastNameSignal.set("");

        assertTrue(firstNameField.isInvalid());
        assertEquals("First and last name are required", binder.validate()
                .getValidationErrors().getFirst().getErrorMessage());
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

        assertTrue(binder.isValid());

        // change of last name triggers validation of first name binding
        lastNameField.setValue("");

        assertTrue(firstNameField.isInvalid());
        assertEquals("First and last name are required", binder.validate()
                .getValidationErrors().getFirst().getErrorMessage());
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

        firstNameField.bindValue(firstNameSignal, firstNameSignal::set);
        lastNameField.bindValue(lastNameSignal, lastNameSignal::set);
        var ageField = new TestTextField();
        ageField.bindValue(ageSignal, ageSignal::set);

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
                                && !firstNameSignal.get().isEmpty()
                                && !emailBinding.value().isEmpty(),
                        ageValidationError)
                .bind("age");
        binder.setBean(item);

        assertTrue(binder.isValid());

        // change of any field triggers validation of age field binding
        emailField.setValue("");

        assertTrue(ageField.isInvalid());
        assertEquals(ageValidationError, ageField.getErrorMessage());

        // rest of the test changes fields one by one to verify that age field
        // validation is re-triggered each time
        emailField.setValue("email");
        assertFalse(ageField.isInvalid());

        firstNameField.setValue("");
        assertTrue(ageField.isInvalid());
        assertEquals(ageValidationError, ageField.getErrorMessage());

        firstNameField.setValue("John");
        lastNameField.setValue("");
        assertTrue(ageField.isInvalid());
        assertEquals(ageValidationError, ageField.getErrorMessage());

        lastNameField.setValue("Smith");
        ageField.setValue("0");
        assertTrue(ageField.isInvalid());
        assertEquals(ageValidationError, ageField.getErrorMessage());

        ageField.setValue("10");
        assertFalse(ageField.isInvalid());
        assertEquals(0, binder.validate().getValidationErrors().size());
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
        assertEquals("", firstNameField.getValue());
        assertEquals("", lastNameField.getValue());

        // Set bean - fields should be populated
        binder.setBean(item);

        assertEquals("Alice", firstNameField.getValue());
        assertEquals("Smith", lastNameField.getValue());
        assertTrue(binder.isValid());

        // Change last name to empty - should trigger validation of first name
        lastNameField.setValue("");

        assertTrue(firstNameField.isInvalid());
        assertEquals("First and last name are required",
                firstNameField.getErrorMessage());

        // Bean should be updated immediately with setBean
        assertEquals("", item.getLastName());
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
        assertEquals("", firstNameField.getValue());
        assertEquals("", lastNameField.getValue());

        // Read bean - fields should be populated
        binder.readBean(item);

        assertEquals("Alice", firstNameField.getValue());
        assertEquals("Smith", lastNameField.getValue());
        assertTrue(binder.isValid());

        // Change last name to empty - should trigger validation of first name
        lastNameField.setValue("");

        assertTrue(firstNameField.isInvalid());
        assertEquals("First and last name are required",
                firstNameField.getErrorMessage());

        // Bean should NOT be updated with readBean
        assertEquals("Smith", item.getLastName());
    }

    // verifies that cross-field validation works correctly with readBean and
    // signal-bound fields
    @Test
    public void crossFieldValidation_readBean_withSignals() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        var firstNameSignal = new ValueSignal<>("");
        var lastNameSignal = new ValueSignal<>("");

        firstNameField.bindValue(firstNameSignal, firstNameSignal::set);
        lastNameField.bindValue(lastNameSignal, lastNameSignal::set);

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        binder.readBean(item);

        assertEquals("Alice", firstNameField.getValue());
        assertEquals("Smith", lastNameField.getValue());
        assertTrue(binder.isValid());

        // Change last name via signal
        lastNameSignal.set("");

        assertTrue(firstNameField.isInvalid());
        assertEquals("First and last name are required",
                firstNameField.getErrorMessage());

        // Bean should NOT be updated with readBean
        assertEquals("Smith", item.getLastName());
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
        assertEquals("Alice", firstNameField.getValue());
        assertEquals("Smith", lastNameField.getValue());

        // Modify fields - should update item1
        firstNameField.setValue("Charlie");
        assertEquals("Charlie", item1.getFirstName());

        // Switch to readBean with item2
        binder.readBean(item2);
        assertEquals("Bob", firstNameField.getValue());
        assertEquals("Jones", lastNameField.getValue());

        // Modify fields - should NOT update item2
        firstNameField.setValue("David");
        assertEquals("Bob", item2.getFirstName());

        // Cross-field validation should still work
        lastNameField.setValue("");
        assertTrue(firstNameField.isInvalid());
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

        assertEquals("Alice", firstNameField.getValue());
        assertEquals("Smith", lastNameField.getValue());
        assertTrue(binder.isValid());

        // Change last name to empty - should trigger validation of first name
        lastNameField.setValue("");

        assertTrue(firstNameField.isInvalid());
        assertEquals("First and last name are required",
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

        firstNameField.bindValue(firstNameSignal, firstNameSignal::set);
        lastNameField.bindValue(lastNameSignal, lastNameSignal::set);

        UI.getCurrent().add(firstNameField, lastNameField);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        binder.readRecord(record);

        assertEquals("Alice", firstNameField.getValue());
        assertEquals("Smith", lastNameField.getValue());
        assertTrue(binder.isValid());

        // Change last name via signal
        lastNameSignal.set("");

        assertTrue(firstNameField.isInvalid());
        assertEquals("First and last name are required",
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
        assertTrue(binder.isValid());
        assertFalse(firstNameField.isInvalid());

        // Set second bean with empty last name - should be invalid
        binder.setBean(item2);
        assertEquals("Bob", firstNameField.getValue());
        assertEquals("", lastNameField.getValue());
        assertFalse(binder.isValid());

        // Validate to trigger error display
        binder.validate();
        assertTrue(firstNameField.isInvalid());

        // Set third bean - should be valid again
        binder.setBean(item3);
        assertEquals("Charlie", firstNameField.getValue());
        assertEquals("Brown", lastNameField.getValue());
        assertTrue(binder.isValid());
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
        assertTrue(firstNameField.isInvalid());

        // Set second bean - validation state should be cleared
        binder.setBean(item2);
        assertEquals("", firstNameField.getValue());
        assertEquals("", lastNameField.getValue());

        // Validate the new bean
        binder.validate();
        assertTrue(firstNameField.isInvalid());
        assertEquals("First and last name are required",
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

        firstNameField.bindValue(firstNameSignal, firstNameSignal::set);
        lastNameField.bindValue(lastNameSignal, lastNameSignal::set);

        var lastNameBinding = binder.forField(lastNameField).bind("lastName");

        binder.forField(firstNameField)
                .withValidator(hasTextValuesValidator(lastNameBinding),
                        "First and last name are required")
                .bind("firstName");

        binder.setBean(item);

        // Fields are NOT attached yet
        assertTrue(binder.isValid());

        // Change last name via signal - should NOT trigger cross-field
        // validation
        // because fields are not attached
        lastNameSignal.set("");

        // First name field should still be valid because the signal change
        // doesn't trigger cross-field validation when detached
        assertFalse(firstNameField.isInvalid());

        // Change last name field directly - should NOT trigger cross-field
        // validation because fields are not attached and ComponentEffect is not
        // active for Binder's internal trigger signal.
        lastNameField.setValue("");

        assertFalse(firstNameField.isInvalid());
        assertEquals("", firstNameField.getErrorMessage());

        // Reset to valid state
        lastNameField.setValue("Smith");
        binder.validate();
        assertFalse(firstNameField.isInvalid());
        assertTrue(binder.isValid());

        // Now attach the fields
        UI.getCurrent().add(firstNameField, lastNameField);

        // Change last name via signal - NOW it should trigger cross-field
        // validation
        lastNameSignal.set("");

        // First name field should be invalid because cross-field validation
        // is triggered automatically when fields are attached
        assertTrue(firstNameField.isInvalid());
        assertEquals("First and last name are required",
                firstNameField.getErrorMessage());
    }

    // verifies that unbind() removes signal registration.
    @Test
    public void unbind_removesSignalRegistration() {
        item.setFirstName("Alice");
        item.setLastName("Smith");

        var firstNameSignal = new ValueSignal<>("");
        var lastNameSignal = new ValueSignal<>("");

        firstNameField.bindValue(firstNameSignal, firstNameSignal::set);
        lastNameField.bindValue(lastNameSignal, lastNameSignal::set);

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
        firstNameSignal.set("");
        assertTrue(lastNameField.isInvalid());

        // Reset to valid state
        firstNameSignal.set("Alice");
        binder.validate();
        assertFalse(lastNameField.isInvalid());

        // Unbind the firstName binding
        firstNameBinding.unbind();

        // Fields stay attached.
        // After unbind, signal registration should be removed
        // Change firstName via signal - should NOT trigger cross-field
        // validation
        firstNameSignal.set("");
        assertFalse(lastNameField.isInvalid());
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

        assertTrue(binder.isValid());

        binder.withValidator(bean -> {
            firstNameSignal.get(); // causes error
            return true;
        }, "Bean level validation with a signal failed");

        assertThrows(UsageTracker.DeniedSignalUsageException.class,
                () -> binder.validate());
        assertThrows(UsageTracker.DeniedSignalUsageException.class,
                () -> binder.isValid());
    }

    @Test
    public void getValidationStatus_signalInitialized() {
        Signal<BinderValidationStatus<Person>> statusSignal = binder
                .getValidationStatus();
        assertNotNull(statusSignal,
                "getValidationStatus() should setup validation status signal");
        assertNotNull(statusSignal.get(),
                "validation status signal value should not be null initially");
    }

    @Test
    public void getValidationStatus_signalIsReadOnly() {
        Signal<BinderValidationStatus<Person>> statusSignal = binder
                .getValidationStatus();
        assertThrows(ClassCastException.class,
                () -> ((ValueSignal<BinderValidationStatus<Person>>) statusSignal)
                        .set(null));
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

        assertTrue(binder.getValidationStatus().get().isOk());

        firstNameField.setValue("");
        assertFalse(binder.getValidationStatus().get().isOk());
        firstNameField.setValue("foo");
        assertTrue(binder.getValidationStatus().get().isOk());
        lastNameField.setValue("");
        assertFalse(binder.getValidationStatus().get().isOk());
        firstNameField.setValue("");
        assertFalse(binder.getValidationStatus().get().isOk());
        firstNameField.setValue("foo");
        assertFalse(binder.getValidationStatus().get().isOk());
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

        assertTrue(
                binder.getValidationStatus().get().getFieldValidationStatuses()
                        .stream().noneMatch(BindingValidationStatus::isError));
        assertTrue(binder.getValidationStatus().get().isOk());

        lastNameField.setValue(""); // change to invalid state

        assertFalse(binder.getValidationStatus().get().isOk());
        var firstNameValidationStatuses = binder.getValidationStatus().get()
                .getFieldValidationStatuses().stream()
                .filter(status -> status.getBinding() == firstNameBinding)
                .toList();
        var otherValidationStatuses = binder.getValidationStatus().get()
                .getFieldValidationStatuses().stream()
                .filter(status -> status.getBinding() != firstNameBinding)
                .toList();
        assertEquals(1, firstNameValidationStatuses.size(),
                "Expected one BindingValidationStatus for first name field");
        assertEquals(1, otherValidationStatuses.size(),
                "Expected one BindingValidationStatus for last name field");
        assertFalse(otherValidationStatuses.get(0).isError(),
                "Expected last name field to NOT have an error");
        assertTrue(firstNameValidationStatuses.get(0).isError(),
                "Expected first name field to have an error");

        lastNameField.setValue("Smith");
        assertTrue(binder.getValidationStatus().get().isOk());
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

        assertTrue(binder.isValid());

        // change of last name doesn't trigger validation of age field binding
        // because converter is run inside UsageTracker.untracked()
        lastNameField.setValue("");

        assertFalse(ageField.isInvalid());
        assertEquals("", ageField.getErrorMessage());
        assertTrue(converterCalls.get() > 0,
                "Converter should be called at least once");
        assertFalse(binder.isValid()); // reruns all validators
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
        Signal.effect(firstNameField, () -> {
            prevStatus.set(binder.getValidationStatus().get().isOk());
            effectCalled.incrementAndGet();
        });

        // change field values to valid and invalid back-and-forth
        // Note: effect counts are higher due to internal signal tracking
        firstNameField.setValue("");
        assertFalse(prevStatus.get());
        assertEquals(3, effectCalled.get());

        firstNameField.setValue("foo");
        assertEquals(5, effectCalled.get());
        assertTrue(prevStatus.get());

        lastNameField.setValue("");
        assertEquals(7, effectCalled.get());
        assertFalse(prevStatus.get());

        firstNameField.setValue("");
        assertFalse(prevStatus.get());
        assertEquals(9, effectCalled.get());
        firstNameField.setValue("foo");
        assertEquals(11, effectCalled.get());
        assertFalse(prevStatus.get(),
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
        Signal.effect(firstNameField, () -> {
            prevStatus.set(binder.getValidationStatus().get().isOk());
        });

        assertFalse(prevStatus.get());

        firstNameField.setValue("foo");
        assertTrue(prevStatus.get());

        var person = new Person();
        person.setFirstName("");

        binderSetup.accept(person);
        assertFalse(prevStatus.get());
    }
}
