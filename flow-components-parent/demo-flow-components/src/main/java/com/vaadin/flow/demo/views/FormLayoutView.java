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

import com.vaadin.data.Binder;
import com.vaadin.data.Binder.Binding;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.Label;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Button;
import com.vaadin.ui.Checkbox;
import com.vaadin.ui.DatePicker;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.FormLayout.FormItem;
import com.vaadin.ui.FormLayout.ResponsiveStep;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

/**
 * Demo view for {@link FormLayout}.
 * 
 * @author Vaadin Ltd
 */
@ComponentDemo(name = "Form Layout", href = "vaadin-form-layout", subcategory = "Layouts")
public class FormLayoutView extends DemoView {

    @Override
    void initView() {
        createResponsiveLayout();
        createFormLayoutWithItems();
        createFormLayoutWithBinder();
    }

    public class Contact implements Serializable {

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
            return firstName + " " + lastName
                    + (birthDate == null ? "" : ", born on " + birthDate)
                    + (phone == null || phone.isEmpty() ? ""
                            : ", phone " + phone
                                    + (doNotCall ? " (don't call me!)"
                                            : " (you can call me)"))
                    + (email == null | email.isEmpty() ? ""
                            : ", e-mail " + email);
        }
    }

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
                new ResponsiveStep("20em", 2),
                new ResponsiveStep("22em", 3));
        // end-source-example
        // @formatter:on

        addCard("A form layout with custom responsive layouting", nameLayout);
    }

    private void createFormLayoutWithItems() {
        // begin-source-example
        // source-example-heading: A form layout with fields wrapped in items
        FormLayout layoutWithFormItems = new FormLayout();

        TextField name = new TextField();
        name.setPlaceholder("John");
        FormItem firstItem = new FormItem(name);
        TextField lastName = new TextField();
        lastName.setPlaceholder("Doe");
        FormItem secondItem = new FormItem(lastName);

        Div firstItemLabelComponent = new Div();
        firstItemLabelComponent.setText("First name");

        Div secondItemLabelComponent = new Div();
        secondItemLabelComponent.setText("Last name");

        firstItem.addToLabel(firstItemLabelComponent);
        secondItem.addToLabel(secondItemLabelComponent);

        layoutWithFormItems.add(firstItem, secondItem);
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

        // Create the form items to proper align the fields
        FormItem firstNameItem = new FormItem(firstName);
        firstNameItem.addToLabel(new Label("First name"));
        FormItem lastNameItem = new FormItem(lastName);
        lastNameItem.addToLabel(new Label("Last name"));
        FormItem birthDateItem = new FormItem(birthDate);
        birthDateItem.addToLabel(new Label("Birthdate"));
        FormItem emailItem = new FormItem(email);
        emailItem.addToLabel(new Label("E-mail"));
        FormItem phoneItem = new FormItem(phone, doNotCall);
        phoneItem.addToLabel(new Label("Phone"));

        // Button bar
        HorizontalLayout actions = new HorizontalLayout();
        actions.add(save, reset);
        save.getStyle().set("marginRight", "10px");

        layoutWithBinder.add(firstNameItem, lastNameItem, birthDateItem,
                emailItem, phoneItem);

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
                infoLabel.setText(errorText);
            }
        });
        reset.addClickListener(event -> {
            // clear fields by setting null
            binder.readBean(null);
            infoLabel.setText("");
            doNotCall.setValue(false);
        });
        // end-source-example

        addCard("A form layout with fields using Binder", layoutWithBinder,
                infoLabel, actions);

    }
}
