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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.tests.data.bean.Person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
class BinderStatusChangeTest extends BinderTestBase<Binder<Person>, Person> {

    private AtomicReference<StatusChangeEvent> event;

    @BeforeEach
    void setUp() {
        binder = new Binder<>();
        item = new Person();
        event = new AtomicReference<>();
    }

    @Test
    void bindBinding_unbound_eventWhenBoundEndnoEventsBeforeBound() {
        binder.addStatusChangeListener(this::statusChanged);

        BindingBuilder<Person, String> binding = binder.forField(nameField);

        nameField.setValue("");
        assertNull(event.get());

        binding.bind(Person::getFirstName, Person::setFirstName);
        verifyEvent();
    }

    @Test
    void bindBinder_unbound_singleEventWhenBound() {
        binder.addStatusChangeListener(this::statusChanged);

        assertNull(event.get());

        binder.setBean(item);

        verifyEvent();
    }

    @Test
    void removeBean_bound_singleEventWhenUnBound() {
        binder.setBean(item);

        binder.addStatusChangeListener(this::statusChanged);

        assertNull(event.get());
        binder.removeBean();
        verifyEvent();
    }

    @Test
    void removeBean_unbound_noEventWhenUnBound() {
        binder.addStatusChangeListener(this::statusChanged);

        assertNull(event.get());
        binder.removeBean();
        assertNull(event.get());
    }

    @Test
    void setValue_bound_singleEventOnSetValue() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.setBean(item);

        binder.addStatusChangeListener(this::statusChanged);

        assertNull(event.get());
        nameField.setValue("foo");
        verifyEvent();
    }

    @Test
    void setValue_severalBoundFieldsAndBoundBinder_singleEventOnSetValue() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.setBean(item);

        binder.addStatusChangeListener(this::statusChanged);

        assertNull(event.get());
        nameField.setValue("foo");
        verifyEvent();
    }

    @Test
    void setInvalidValue_bound_singleEventOnSetValue() {
        binder.forField(nameField).withValidator(name -> false, "")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.setBean(item);

        binder.addStatusChangeListener(this::statusChanged);

        assertNull(event.get());
        nameField.setValue("foo");
        verifyEvent(true);
    }

    @Test
    void setInvalidBean_bound_singleEventOnSetValue() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.setBean(item);

        binder.withValidator(Validator.from(bean -> false, ""));

        binder.addStatusChangeListener(this::statusChanged);

        assertNull(event.get());
        nameField.setValue("foo");
        verifyEvent(true);
    }

    @Test
    void readBean_hasBindings_singleEventOnLoad() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.readBean(item);
        verifyEvent();
    }

    @Test
    void readBean_hasSeveralBindings_singleEventOnLoad() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.readBean(item);
        verifyEvent();
    }

    @Test
    void readBean_hasNoBindings_singleEvent() {
        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.readBean(item);
        verifyEvent();
    }

    @Test
    void writeBean_hasNoBindings_singleEvent() throws ValidationException {
        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBean(item);
        verifyEvent();
    }

    @Test
    void writeBeanIfValid_hasNoBindings_singleEvent() {
        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBeanIfValid(item);
        verifyEvent();
    }

    @Test
    void writeBean_hasBindings_singleEvent() throws ValidationException {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.readBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBean(item);
        verifyEvent();
    }

    @Test
    void writeBean_hasSeveralBindings_singleEvent() throws ValidationException {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.readBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBean(item);
        verifyEvent();
    }

    @Test
    void writeBeanIfValid_hasBindings_singleEvent() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.readBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBeanIfValid(item);
        verifyEvent();
    }

    @Test
    void writeBeanIfValid_hasSeveralBindings_singleEvent() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.readBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBeanIfValid(item);
        verifyEvent();
    }

    @Test
    void writeBeanInvalidValue_hasBindings_singleEvent() {
        binder.forField(nameField).withValidator(name -> false, "")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.readBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        try {
            binder.writeBean(item);
        } catch (ValidationException ignore) {
        }
        verifyEvent(true);
    }

    @Test
    void writeBeanIfValid_invalidValueAndBinderHasBindings_singleEvent() {
        binder.forField(nameField).withValidator(name -> false, "")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.readBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBeanIfValid(item);
        verifyEvent(true);
    }

    @Test
    void writeBeanIfValid_invalidValueAndBinderHasSeveralBindings_singleEvent() {
        binder.forField(nameField).withValidator(name -> false, "")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.readBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBeanIfValid(item);
        verifyEvent(true);
    }

    @Test
    void writeBeanInvalidBean_hasBindings_singleEvent() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.readBean(item);
        binder.withValidator(Validator.from(person -> false, ""));

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        try {
            binder.writeBean(item);
        } catch (ValidationException ignore) {
        }
        verifyEvent(true);
    }

    @Test
    void writeBeanIfValid_invalidBeanAndBinderHasBindings_singleEvent() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.readBean(item);
        binder.withValidator(Validator.from(person -> false, ""));

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBeanIfValid(item);
        verifyEvent(true);
    }

    @Test
    void writeValidBean_hasBindings_singleEvent() throws ValidationException {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.readBean(item);
        binder.withValidator(Validator.from(person -> true, ""));

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBean(item);
        verifyEvent();
    }

    @Test
    void writeBeanIfValid_validBeanAndBinderHasBindings_singleEvent() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.readBean(item);
        binder.withValidator(Validator.from(person -> true, ""));

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());
        binder.writeBeanIfValid(item);
        verifyEvent();
    }

    @Test
    void validateBinder_noValidationErrors_statusEventWithoutErrors() {
        binder.forField(nameField).bind(Person::getFirstName,
                Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.setBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());

        binder.validate();
        verifyEvent();
    }

    @Test
    void validateBinder_validationErrors_statusEventWithError() {
        binder.forField(nameField).withValidator(name -> false, "")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.setBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());

        binder.validate();
        verifyEvent(true);
    }

    @Test
    void validateBinding_noValidationErrors_statusEventWithoutErrors() {
        Binding<Person, String> binding = binder.forField(nameField)
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.setBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());

        binding.validate();
        verifyEvent();
    }

    @Test
    void validateBinding_validationErrors_statusEventWithError() {
        Binding<Person, String> binding = binder.forField(nameField)
                .withValidator(name -> false, "")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(ageField)
                .withConverter(new StringToIntegerConverter(""))
                .bind(Person::getAge, Person::setAge);
        binder.setBean(item);

        binder.addStatusChangeListener(this::statusChanged);
        assertNull(event.get());

        binding.validate();
        verifyEvent(true);
    }

    private void verifyEvent() {
        verifyEvent(false);
    }

    private void verifyEvent(boolean validationErrors) {
        StatusChangeEvent statusChangeEvent = event.get();
        assertNotNull(statusChangeEvent);
        assertEquals(binder, statusChangeEvent.getBinder());
        assertEquals(binder, statusChangeEvent.getSource());
        assertEquals(validationErrors, statusChangeEvent.hasValidationErrors());
    }

    private void statusChanged(StatusChangeEvent evt) {
        assertNull(event.get());
        event.set(evt);
    }
}
