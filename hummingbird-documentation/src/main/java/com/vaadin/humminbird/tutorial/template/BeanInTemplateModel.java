/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.humminbird.tutorial.template;

import com.vaadin.annotations.EventHandler;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.ui.Template;

@CodeFor("tutorial-template-model-bean.asciidoc")
public class BeanInTemplateModel {
    public class Person {
        private String firstName, lastName;
        private int age;

        public Person() {
            // Needed for TemplateModel
        }

        public Person(String firstName, String lastName, int age) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
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

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public interface FormModel extends TemplateModel {
        public void setPerson(Person person);

        public Person getPerson();
    }

    public class Form extends Template {
        public Form() {
            Person person = new Person("John", "Doe", 82);
            getModel().setPerson(person);
        }

        @Override
        protected FormModel getModel() {
            return (FormModel) super.getModel();
        }

        @EventHandler
        public void setNameToJeff() {
            getModel().getPerson().setFirstName("Jeff");
        }
    }

}
