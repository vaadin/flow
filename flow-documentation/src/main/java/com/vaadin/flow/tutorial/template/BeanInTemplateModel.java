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
package com.vaadin.flow.tutorial.template;

import com.vaadin.annotations.ClientDelegate;
import com.vaadin.annotations.Exclude;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.ui.AngularTemplate;

@Deprecated
@CodeFor("deprecated/tutorial-template-model-bean.asciidoc")
public class BeanInTemplateModel {
    public class Person {
        private String firstName, lastName;
        private int age;

        private Long id;

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

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public interface FormModel extends TemplateModel {
        @Exclude("id")
        public void setPerson(Person person);

        public Person getPerson();
    }

    public class Form extends AngularTemplate {
        public Form() {
            Person person = new Person("John", "Doe", 82);
            getModel().setPerson(person);
        }

        @Override
        protected FormModel getModel() {
            return (FormModel) super.getModel();
        }

        @ClientDelegate
        public void setNameToJeff() {
            getModel().getPerson().setFirstName("Jeff");
        }
    }

}
