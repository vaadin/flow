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

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.function.ValueProvider;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.textfield.TextField;

@CodeFor("binding-data/tutorial-flow-components-binder.asciidoc")
public class BinderBasic {

    private TextField titleField;
    private TextField nameField;

    private Binder<Person> binder = new Binder<>();

    public void bindField() {
        Binder<Person> binder = new Binder<>();

        TextField titleField = new TextField();

        // Start by defining the Field instance to use
        binder.forField(titleField)
                // Finalize by doing the actual binding to the Person class
                .bind(
                        // Callback that loads the title from a person instance
                        Person::getTitle,
                        // Callback that saves the title in a person instance
                        Person::setTitle);

        TextField nameField = new TextField();

        // Shorthand for cases without extra configuration
        binder.bind(nameField, Person::getName, Person::setName);
    }

    public void readWriteBean() {
        // The person to edit
        // Would be loaded from the backend in a real application
        Person person = new Person("John Doe", 1957);

        // Updates the value in each bound field component
        binder.readBean(person);

        // @formatter:off
        Button saveButton = new Button("Save",
                event -> {
                    try {
                        binder.writeBean(person);
                        // A real application would also save the updated person
                        // using the application's backend
                    } catch (ValidationException e) {
                        notifyValidationException(e);
                    }
                });

        // Updates the fields again with the previously saved values
        Button resetButton = new Button("Reset",
                event -> binder.readBean(person));
        //@formatter:on
    }

    public void lambdaCllbacks() {
        // @formatter:off
        // With lambda expressions
        binder.bind(titleField,
                person -> person.getTitle(),
                (person, title) -> person.setTitle(title));

        // With explicit callback interface instances
        binder.bind(nameField,
                new ValueProvider<Person, String>() {
            @Override
            public String apply(Person person) {
                return person.getName();
            }
        },
                new Setter<Person, String>() {
            @Override
            public void accept(Person person, String name) {
                person.setName(name);
            }
        });
        //@formatter:on
    }

    private void notifyValidationException(ValidationException exception) {
        System.out.println("Person could not be saved, "
                + "please check error messages for each field.");
    }

}
