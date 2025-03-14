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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.data.binder.testcomponents.TestLabel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.BindingValidationStatus.Status;
import com.vaadin.flow.tests.data.bean.Person;

public class BinderValidationStatusTest
        extends BinderTestBase<Binder<Person>, Person> {
    private final Map<HasValue<?, ?>, String> componentErrors = new HashMap<>();

    protected final static BindingValidationStatusHandler NOOP = event -> {
    };

    @Before
    public void setUp() {
        binder = new Binder<Person>() {
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
                    Assert.assertNull(statusCapture.get());
                    statusCapture.set(evt);
                });
        binding.bind(Person::getFirstName, Person::setFirstName);

        nameField.setValue("");

        // First validation fails => should be event with ERROR status and
        // message
        binder.validate();

        Assert.assertNotNull(statusCapture.get());
        BindingValidationStatus<?> evt = statusCapture.get();
        Assert.assertEquals(Status.ERROR, evt.getStatus());
        Assert.assertEquals(EMPTY_ERROR_MESSAGE, evt.getMessage().get());
        Assert.assertEquals(nameField, evt.getField());

        statusCapture.set(null);
        nameField.setValue("foo");

        statusCapture.set(null);
        // Second validation succeeds => should be event with OK status and
        // no message
        binder.validate();

        evt = statusCapture.get();
        Assert.assertNotNull(evt);
        Assert.assertEquals(Status.OK, evt.getStatus());
        Assert.assertFalse(evt.getMessage().isPresent());
        Assert.assertEquals(nameField, evt.getField());
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
        Assert.assertEquals(EMPTY_ERROR_MESSAGE, label.getText());

        nameField.setValue("foo");

        // Second validation succeeds => should be event with OK status and
        // no message
        binding.validate();

        assertVisible(label, false);
        Assert.assertEquals("", label.getText());
    }

    @Test
    public void bindingWithStatusLabel_defaultStatusHandlerIsReplaced() {
        TestLabel label = new TestLabel();

        Binding<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty).withStatusLabel(label)
                .bind(Person::getFirstName, Person::setFirstName);

        Assert.assertNull(componentErrors.get(nameField));

        nameField.setValue("");

        // First validation fails => should be event with ERROR status and
        // message
        binding.validate();

        // default behavior should update component error for the nameField
        Assert.assertNull(componentErrors.get(nameField));
    }

    @Test(expected = IllegalStateException.class)
    public void bindingWithStatusHandler_addAfterBound() {
        BindingBuilder<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty);
        binding.bind(Person::getFirstName, Person::setFirstName);

        binding.withValidationStatusHandler(evt -> Assert.fail());
    }

    @Test(expected = IllegalStateException.class)
    public void bindingWithStatusLabel_addAfterBound() {
        TestLabel label = new TestLabel();

        BindingBuilder<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty);
        binding.bind(Person::getFirstName, Person::setFirstName);

        binding.withStatusLabel(label);
    }

    @Test(expected = IllegalStateException.class)
    public void bindingWithStatusLabel_setAfterHandler() {
        TestLabel label = new TestLabel();

        BindingBuilder<Person, String> binding = binder.forField(nameField);

        binding.withValidationStatusHandler(NOOP);

        binding.withStatusLabel(label);
    }

    @Test(expected = IllegalStateException.class)
    public void bindingWithStatusHandler_setAfterLabel() {
        TestLabel label = new TestLabel();

        BindingBuilder<Person, String> binding = binder.forField(nameField);

        binding.withStatusLabel(label);

        binding.withValidationStatusHandler(NOOP);
    }

    @Test(expected = IllegalStateException.class)
    public void bindingWithStatusHandler_setAfterOtherHandler() {

        BindingBuilder<Person, String> binding = binder.forField(nameField);

        binding.withValidationStatusHandler(NOOP);

        binding.withValidationStatusHandler(NOOP);
    }

    //
    // Binder-level status handler
    //

    @Test
    public void binderWithStatusHandler_fieldValidationNoBeanValidation_handlerGetsStatusUpdates() {
        AtomicReference<BinderValidationStatus<?>> statusCapture = new AtomicReference<>();
        binder.forField(nameField).withValidator(notEmpty)
                .withValidationStatusHandler(evt -> Assert.fail(
                        "Using a custom status change handler so no change should end up here"))
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField).withConverter(stringToInteger)
                .withValidator(notNegative)
                .withValidationStatusHandler(evt -> Assert.fail(
                        "Using a custom status change handler so no change should end up here"))
                .bind(Person::getAge, Person::setAge);

        binder.setValidationStatusHandler(statusCapture::set);
        binder.setBean(item);
        Assert.assertNull(componentErrors.get(nameField));

        nameField.setValue("");
        ageField.setValue("5");

        // First binding validation fails => should be result with ERROR status
        // and message
        BinderValidationStatus<Person> status2 = binder.validate();
        BinderValidationStatus<?> status = statusCapture.get();
        Assert.assertSame(status2, status);

        Assert.assertNull(componentErrors.get(nameField));

        List<BindingValidationStatus<?>> bindingStatuses = status
                .getFieldValidationStatuses();
        Assert.assertNotNull(bindingStatuses);
        Assert.assertEquals(1, status.getFieldValidationErrors().size());
        Assert.assertEquals(2, bindingStatuses.size());

        BindingValidationStatus<?> r = bindingStatuses.get(0);
        Assert.assertTrue(r.isError());
        Assert.assertEquals(EMPTY_ERROR_MESSAGE, r.getMessage().get());
        Assert.assertEquals(nameField, r.getField());

        r = bindingStatuses.get(1);
        Assert.assertFalse(r.isError());
        Assert.assertFalse(r.getMessage().isPresent());
        Assert.assertEquals(ageField, r.getField());

        Assert.assertEquals(0, status.getBeanValidationResults().size());
        Assert.assertEquals(0, status.getBeanValidationErrors().size());

        nameField.setValue("foo");
        ageField.setValue("");

        statusCapture.set(null);
        // Second validation succeeds => should be result with OK status and
        // no message, and error result for age
        binder.validate();

        status = statusCapture.get();
        bindingStatuses = status.getFieldValidationStatuses();
        Assert.assertEquals(1, status.getFieldValidationErrors().size());
        Assert.assertEquals(2, bindingStatuses.size());

        r = bindingStatuses.get(0);
        Assert.assertFalse(r.isError());
        Assert.assertFalse(r.getMessage().isPresent());
        Assert.assertEquals(nameField, r.getField());

        r = bindingStatuses.get(1);
        Assert.assertTrue(r.isError());
        Assert.assertEquals(NOT_NUMBER_ERROR_MESSAGE, r.getMessage().get());
        Assert.assertEquals(ageField, r.getField());

        Assert.assertEquals(0, status.getBeanValidationResults().size());
        Assert.assertEquals(0, status.getBeanValidationErrors().size());

        statusCapture.set(null);
        // binding validations pass, binder validation fails
        ageField.setValue("0");
        binder.validate();

        status = statusCapture.get();
        bindingStatuses = status.getFieldValidationStatuses();
        Assert.assertEquals(0, status.getFieldValidationErrors().size());
        Assert.assertEquals(2, bindingStatuses.size());

        Assert.assertEquals(0, status.getBeanValidationResults().size());
        Assert.assertEquals(0, status.getBeanValidationErrors().size());
    }

    @Test
    public void binderWithStatusHandler_fieldAndBeanLevelValidation_handlerGetsStatusUpdates() {
        AtomicReference<BinderValidationStatus<?>> statusCapture = new AtomicReference<>();
        binder.forField(nameField).withValidator(notEmpty)
                .withValidationStatusHandler(evt -> Assert.fail(
                        "Using a custom status change handler so no change should end up here"))
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField).withConverter(stringToInteger)
                .withValidator(notNegative)
                .withValidationStatusHandler(evt -> Assert.fail(
                        "Using a custom status change handler so no change should end up here"))
                .bind(Person::getAge, Person::setAge);
        binder.withValidator(
                bean -> !bean.getFirstName().isEmpty() && bean.getAge() > 0,
                "Need first name and age");

        binder.setValidationStatusHandler(statusCapture::set);
        binder.setBean(item);
        Assert.assertNull(componentErrors.get(nameField));

        nameField.setValue("");
        ageField.setValue("5");

        // First binding validation fails => should be result with ERROR status
        // and message
        BinderValidationStatus<Person> status2 = binder.validate();
        BinderValidationStatus<?> status = statusCapture.get();
        Assert.assertSame(status2, status);

        Assert.assertNull(componentErrors.get(nameField));

        List<BindingValidationStatus<?>> bindingStatuses = status
                .getFieldValidationStatuses();
        Assert.assertNotNull(bindingStatuses);
        Assert.assertEquals(1, status.getFieldValidationErrors().size());
        Assert.assertEquals(2, bindingStatuses.size());

        BindingValidationStatus<?> r = bindingStatuses.get(0);
        Assert.assertTrue(r.isError());
        Assert.assertEquals(EMPTY_ERROR_MESSAGE, r.getMessage().get());
        Assert.assertEquals(nameField, r.getField());

        r = bindingStatuses.get(1);
        Assert.assertFalse(r.isError());
        Assert.assertFalse(r.getMessage().isPresent());
        Assert.assertEquals(ageField, r.getField());

        Assert.assertEquals(0, status.getBeanValidationResults().size());
        Assert.assertEquals(0, status.getBeanValidationErrors().size());

        nameField.setValue("foo");
        ageField.setValue("");

        statusCapture.set(null);
        // Second validation succeeds => should be result with OK status and
        // no message, and error result for age
        binder.validate();

        status = statusCapture.get();
        bindingStatuses = status.getFieldValidationStatuses();
        Assert.assertEquals(1, status.getFieldValidationErrors().size());
        Assert.assertEquals(2, bindingStatuses.size());

        r = bindingStatuses.get(0);
        Assert.assertFalse(r.isError());
        Assert.assertFalse(r.getMessage().isPresent());
        Assert.assertEquals(nameField, r.getField());

        r = bindingStatuses.get(1);
        Assert.assertTrue(r.isError());
        Assert.assertEquals(NOT_NUMBER_ERROR_MESSAGE, r.getMessage().get());
        Assert.assertEquals(ageField, r.getField());

        Assert.assertEquals(0, status.getBeanValidationResults().size());
        Assert.assertEquals(0, status.getBeanValidationErrors().size());

        statusCapture.set(null);
        // binding validations pass, binder validation fails
        ageField.setValue("0");
        binder.validate();

        status = statusCapture.get();
        bindingStatuses = status.getFieldValidationStatuses();
        Assert.assertEquals(0, status.getFieldValidationErrors().size());
        Assert.assertEquals(2, bindingStatuses.size());

        Assert.assertEquals(1, status.getBeanValidationResults().size());
        Assert.assertEquals(1, status.getBeanValidationErrors().size());

        Assert.assertEquals("Need first name and age",
                status.getBeanValidationErrors().get(0).getErrorMessage());
    }

    @Test
    public void binderWithStatusHandler_defaultStatusHandlerIsReplaced() {
        Binding<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty).withValidationStatusHandler(evt -> {
                }).bind(Person::getFirstName, Person::setFirstName);

        Assert.assertNull(componentErrors.get(nameField));

        nameField.setValue("");

        // First validation fails => should be event with ERROR status and
        // message
        binding.validate();

        // no component error since default handler is replaced
        Assert.assertNull(componentErrors.get(nameField));
    }

    @Test(expected = IllegalStateException.class)
    public void binderWithStatusHandler_addAfterBound() {
        BindingBuilder<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty);
        binding.bind(Person::getFirstName, Person::setFirstName);

        binding.withValidationStatusHandler(evt -> Assert.fail());
    }

    @Test(expected = IllegalStateException.class)
    public void binderWithStatusLabel_addAfterBound() {
        TestLabel label = new TestLabel();

        BindingBuilder<Person, String> binding = binder.forField(nameField)
                .withValidator(notEmpty);
        binding.bind(Person::getFirstName, Person::setFirstName);

        binding.withStatusLabel(label);
    }

    @Test(expected = IllegalStateException.class)
    public void binderWithStatusLabel_setAfterHandler() {
        TestLabel label = new TestLabel();

        BindingBuilder<Person, String> binding = binder.forField(nameField);
        binding.bind(Person::getFirstName, Person::setFirstName);

        binder.setValidationStatusHandler(event -> {
        });

        binder.setStatusLabel(label);
    }

    @Test(expected = IllegalStateException.class)
    public void binderWithStatusHandler_setAfterLabel() {
        TestLabel label = new TestLabel();

        BindingBuilder<Person, String> binding = binder.forField(nameField);
        binding.bind(Person::getFirstName, Person::setFirstName);

        binder.setStatusLabel(label);

        binder.setValidationStatusHandler(event -> {
        });
    }

    @Test(expected = NullPointerException.class)
    public void binderWithNullStatusHandler_throws() {
        binder.setValidationStatusHandler(null);
    }

    @Test
    public void binderWithStatusHandler_replaceHandler() {
        AtomicReference<BinderValidationStatus<?>> capture = new AtomicReference<>();

        BindingBuilder<Person, String> binding = binder.forField(nameField);
        binding.bind(Person::getFirstName, Person::setFirstName);

        binder.setValidationStatusHandler(results -> Assert.fail());
        binder.setValidationStatusHandler(capture::set);

        nameField.setValue("foo");
        binder.validate();

        List<BindingValidationStatus<?>> results = capture.get()
                .getFieldValidationStatuses();
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        Assert.assertFalse(results.get(0).isError());
        assertValidField(nameField);
    }

    private void assertVisible(TestLabel label, boolean visible) {
        if (visible) {
            Assert.assertNull(label.getStyle().get("display"));
        } else {
            Assert.assertEquals("none", label.getStyle().get("display"));
        }
    }

}
