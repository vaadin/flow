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
package com.vaadin.flow.tutorial.databinding;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("binding-data/tutorial-flow-components-binder-load.asciidoc")
public class BinderLoadSave {

    private Binder<Person> binder = new Binder<>();

    private static class MyBackend {
        private static void updatePersonInDatabase(Person person) {

        }
    }

    public void readBean() {
        Person person = new Person("John Doe", 1957);

        binder.readBean(person);
    }

    public void validate() {
        // This will make all current validation errors visible
        BinderValidationStatus<Person> status = binder.validate();

        if (status.hasErrors()) {
            notifyValidationErrors(status.getValidationErrors());
        }
    }

    public void writeBean() {
        Person person = new Person("John Doe", 1957);

        try {
            binder.writeBean(person);
            MyBackend.updatePersonInDatabase(person);
        } catch (ValidationException e) {
            notifyValidationErrors(e.getValidationErrors());
        }
    }

    public void writeIfValid() {
        Person person = new Person("John Doe", 1957);

        boolean saved = binder.writeBeanIfValid(person);
        if (saved) {
            MyBackend.updatePersonInDatabase(person);
        } else {
            notifyValidationErrors(binder.validate().getValidationErrors());
        }
    }

    public void statusChangeListener() {
        Button saveButton = new Button("Save");
        Button resetButton = new Button("Reset");

        binder.addStatusChangeListener(event -> {
            boolean isValid = event.getBinder().isValid();
            boolean hasChanges = event.getBinder().hasChanges();

            saveButton.setDisabled(!hasChanges || !isValid);
            resetButton.setDisabled(!hasChanges);
        });
    }

    public void autoSave() {
        Binder<Person> binder = new Binder<>();

        // Field binding configuration omitted, it should be done here

        Person person = new Person("John Doe", 1957);

        // Loads the values from the person instance
        // Sets person to be updated when any bound field is updated
        binder.setBean(person);

        Button saveButton = new Button("Save", event -> {
            if (binder.validate().isOk()) {
                // person is always up-to-date as long as there are no
                // validation errors

                MyBackend.updatePersonInDatabase(person);
            }
        });
    }

    private void notifyValidationErrors(List<ValidationResult> results) {

    }
}
