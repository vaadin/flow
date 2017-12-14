/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.binder;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatusHandler;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.html.Label;

@CodeFor("binding-data/tutorial-flow-components-binder-beans.asciidoc")
public class BinderBeans {

    private TextField streetAddressField;
    private TextField nameField;
    private TextField yearOfBirthField;

    public class Person {
        @Max(2000)
        private int yearOfBirth;

        // Non-standard constraint provided by Hibernate Validator
        @NotEmpty
        private String name;

        // + other fields, constructors, setters, and getters
    }

    public void bindSubProprties() {
        Binder<Person> binder = new Binder<>(Person.class);

        // Bind based on property name
        binder.bind(nameField, "name");
        // Bind based on sub property path
        binder.bind(streetAddressField, "address.street");
        // Bind using forField for additional configuration
        binder.forField(yearOfBirthField)
                .withConverter(
                        new StringToIntegerConverter("Please enter a number"))
                .bind("yearOfBirth");
    }

    public void beanBinder() {
        // @formatter:off
        BeanValidationBinder<Person> binder = new BeanValidationBinder<>(Person.class);

        binder.bind(nameField, "name");
        binder.forField(yearOfBirthField)
        .withConverter(
                new StringToIntegerConverter("Please enter a number"))
        .bind("yearOfBirth");
        // @formatter:on
    }

    public void statusLabel() {
        Label formStatusLabel = new Label();

        Binder<Person> binder = new Binder<>(Person.class);

        binder.setStatusLabel(formStatusLabel);

        // Continue by binding fields
    }

    public void statusHandler() {
        Label formStatusLabel = new Label();
        Binder<Person> binder = new Binder<>(Person.class);
        // @formatter:off
        BinderValidationStatusHandler<Person> defaultHandler = binder
                .getValidationStatusHandler();

        binder.setValidationStatusHandler(status -> {
            // create an error message on failed bean level validations
            List<ValidationResult> errors = status
                    .getBeanValidationErrors();

            // collect all bean level error messages into a single string,
            // separating each message with a <br> tag
            String errorMessage = errors.stream()
                    .map(ValidationResult::getErrorMessage)
                    // sanitize the individual error strings to avoid code
                    // injection
                    // since we are displaying the resulting string as HTML
                    .map(errorString -> Jsoup.clean(errorString,
                            Whitelist.simpleText()))
                    .collect(Collectors.joining("<br>"));

            // finally, display all bean level validation errors in a single
            // label
            formStatusLabel.getElement().setProperty("innerHTML", errorMessage);
            setVisible(formStatusLabel, !errorMessage.isEmpty());

            // Let the default handler show messages for each field
            defaultHandler.statusChange(status);
        });
        // @formatter:on
    }

    private void setVisible(Label label, boolean visible) {

    }
}
