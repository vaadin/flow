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
package com.vaadin.flow.demo.views;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.router.Route;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.checkbox.Checkbox;
import com.vaadin.ui.datepicker.DatePicker;
import com.vaadin.ui.formlayout.FormLayout;
import com.vaadin.ui.formlayout.FormLayout.FormItem;
import com.vaadin.ui.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.ui.layout.HorizontalLayout;
import com.vaadin.ui.textfield.TextField;

/**
 * Demo view for {@link FormLayout}.
 *
 * @author Vaadin Ltd
 */
@Route(value = "vaadin-form-layout", layout = MainLayout.class)
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-text-field.html")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-button.html")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-form-layout.html")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-date-picker.html")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-checkbox.html")
@ComponentDemo(name = "Form Layout", subcategory = "Layouts")
public class FormLayoutView extends DemoView {

    @Override
    protected void initView() {
        createResponsiveLayout();
        createFormLayoutWithItems();
        createFormLayoutWithBinder();
        createCompositeLayout();
    }

    /**
     * Example Bean for the Form with Binder.
     */
    private static class Contact implements Serializable {

        private String firstName = "";
        private String lastName = "";
        private String phone = "";
        private String email = "";
        private LocalDate birthDate;
        private boolean doNotCall;

        public boolean isDoNotCall() {
            return doNotCall;
        }

        public void setDoNotCall(boolean doNotCall) {
            this.doNotCall = doNotCall;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public LocalDate getBirthDate() {
            return birthDate;
        }

        public void setBirthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(firstName).append(" ").append(lastName);
            if (birthDate != null) {
                builder.append(", born on ").append(birthDate);
            }
            if (phone != null && !phone.isEmpty()) {
                builder.append(", phone ").append(phone);
                if (doNotCall) {
                    builder.append(" (don't call me!)");
                } else {
                    builder.append(" (you can call me)");
                }
            }
            if (email != null && !email.isEmpty()) {
                builder.append(", e-mail ").append(email);
            }
            return builder.toString();
        }
    }

    // begin-source-example
    // source-example-heading: Using form layout inside a composite
    // You can create a custom layout that internally uses FormLayout
    public class MyCustomLayout extends Composite<FormLayout> {

        public void addItemWithLabel(String label, Component... items) {
            Div itemWrapper = new Div();
            // Wrap the given items into a single div
            itemWrapper.add(items);
            // getContent() returns a wrapped FormLayout
            getContent().addFormItem(itemWrapper, label);
        }
    }
    // end-source-example

    private void createResponsiveLayout() {
        // @formatter:off
        // begin-source-example
        // source-example-heading: A form layout with custom responsive layouting
        FormLayout nameLayout = new FormLayout();

        TextField titleField = new TextField();
        titleField.setLabel("Title");
        titleField.setPlaceholder("Sir");
        TextField firstNameField = new TextField();
        firstNameField.setLabel("First name");
        firstNameField.setPlaceholder("John");
        TextField lastNameField = new TextField();
        lastNameField.setLabel("Last name");
        lastNameField.setPlaceholder("Doe");

        nameLayout.add(titleField, firstNameField, lastNameField);

        nameLayout.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("21em", 2),
                new ResponsiveStep("22em", 3));
        // end-source-example
        // @formatter:on

        addCard("A form layout with custom responsive layouting", nameLayout);
    }

    private void createFormLayoutWithItems() {
        // begin-source-example
        // source-example-heading: A form layout with fields wrapped in items
        FormLayout layoutWithFormItems = new FormLayout();

        TextField firstName = new TextField();
        firstName.setPlaceholder("John");
        layoutWithFormItems.addFormItem(firstName, "First name");

        TextField lastName = new TextField();
        lastName.setPlaceholder("Doe");
        layoutWithFormItems.addFormItem(lastName, "Last name");
        // end-source-example

        addCard("A form layout with fields wrapped in items",
                layoutWithFormItems);
    }

    private void createFormLayoutWithBinder() {
        // begin-source-example
        // source-example-heading: A form layout with fields using Binder
        FormLayout layoutWithBinder = new FormLayout();
        Binder<Contact> binder = new Binder<>();

        // The object that will be edited
        Contact contactBeingEdited = new Contact();

        // Create the fields
        TextField firstName = new TextField();
        TextField lastName = new TextField();
        TextField phone = new TextField();
        TextField email = new TextField();
        DatePicker birthDate = new DatePicker();
        Checkbox doNotCall = new Checkbox("Do not call");
        Label infoLabel = new Label();
        Button save = new Button("Save");
        Button reset = new Button("Reset");

        layoutWithBinder.addFormItem(firstName, "First name");
        layoutWithBinder.addFormItem(lastName, "Last name");
        layoutWithBinder.addFormItem(birthDate, "Birthdate");
        layoutWithBinder.addFormItem(email, "E-mail");
        FormItem phoneItem = layoutWithBinder.addFormItem(phone, "Phone");
        phoneItem.add(doNotCall);

        // Button bar
        HorizontalLayout actions = new HorizontalLayout();
        actions.add(save, reset);
        save.getStyle().set("marginRight", "10px");

        SerializablePredicate<String> phoneOrEmailPredicate = value -> !phone
                .getValue().trim().isEmpty()
                || !email.getValue().trim().isEmpty();

        // E-mail and phone have specific validators
        Binding<Contact, String> emailBinding = binder.forField(email)
                .withValidator(phoneOrEmailPredicate,
                        "Both phone and email cannot be empty")
                .withValidator(new EmailValidator("Incorrect email address"))
                .bind(Contact::getEmail, Contact::setEmail);

        Binding<Contact, String> phoneBinding = binder.forField(phone)
                .withValidator(phoneOrEmailPredicate,
                        "Both phone and email cannot be empty")
                .bind(Contact::getPhone, Contact::setPhone);

        // Trigger cross-field validation when the other field is changed
        email.addValueChangeListener(event -> phoneBinding.validate());
        phone.addValueChangeListener(event -> emailBinding.validate());

        // First name and last name are required fields
        firstName.setRequiredIndicatorVisible(true);
        lastName.setRequiredIndicatorVisible(true);

        binder.forField(firstName)
                .withValidator(new StringLengthValidator(
                        "Please add the first name", 1, null))
                .bind(Contact::getFirstName, Contact::setFirstName);
        binder.forField(lastName)
                .withValidator(new StringLengthValidator(
                        "Please add the last name", 1, null))
                .bind(Contact::getLastName, Contact::setLastName);

        // Birthdate and doNotCall don't need any special validators
        binder.bind(doNotCall, Contact::isDoNotCall, Contact::setDoNotCall);
        binder.bind(birthDate, Contact::getBirthDate, Contact::setBirthDate);

        // Click listeners for the buttons
        save.addClickListener(event -> {
            if (binder.writeBeanIfValid(contactBeingEdited)) {
                infoLabel.setText("Saved bean values: " + contactBeingEdited);
            } else {
                BinderValidationStatus<Contact> validate = binder.validate();
                String errorText = validate.getFieldValidationStatuses()
                        .stream().filter(BindingValidationStatus::isError)
                        .map(BindingValidationStatus::getMessage)
                        .map(Optional::get).distinct()
                        .collect(Collectors.joining(", "));
                infoLabel.setText("There are errors: " + errorText);
            }
        });
        reset.addClickListener(event -> {
            // clear fields by setting null
            binder.readBean(null);
            infoLabel.setText("");
            doNotCall.setValue(false);
        });
        // end-source-example

        infoLabel.setId("binder-info");
        firstName.setId("binder-first-name");
        lastName.setId("binder-last-name");
        phone.setId("binder-phone");
        email.setId("binder-email");
        birthDate.setId("binder-birth-date");
        doNotCall.setId("binder-do-not-call");
        save.setId("binder-save");
        reset.setId("binder-reset");

        addCard("A form layout with fields using Binder", layoutWithBinder,
                actions, infoLabel);

    }

    private void createCompositeLayout() {
        // begin-source-example
        // source-example-heading: Using form layout inside a composite
        // And then just use it like a regular component
        MyCustomLayout layout = new MyCustomLayout();
        TextField name = new TextField();
        TextField email = new TextField();
        Checkbox emailUpdates = new Checkbox("E-mail me updates");

        layout.addItemWithLabel("Name", name);
        // Both the email field and the emailUpdates checkbox are wrapped inside
        // the same form item
        layout.addItemWithLabel("E-mail", email, emailUpdates);
        // end-source-example

        addCard("Using form layout inside a composite", layout);
    }
}
