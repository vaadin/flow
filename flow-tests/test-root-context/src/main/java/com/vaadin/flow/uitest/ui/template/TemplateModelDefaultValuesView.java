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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.AngularTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateModelDefaultValuesView", layout = ViewTestLayout.class)
public class TemplateModelDefaultValuesView extends AngularTemplate {

    public interface Model extends TemplateModel {
        AllTypes getDefaultsInBean();

        void setDefaultsInBean(AllTypes defaults);

        void setBooleanValue(boolean booleanValue);

        void setBooleanObject(Boolean booleanObject);

        void setIntValue(int intValue);

        void setIntObject(Integer intObject);

        void setDoubleValue(double doubleValue);

        void setDoubleObject(Double doubleObject);

        void setString(String string);

        void setPerson(Person person);

        void setDefinedPerson(Person definedPerson);

    }

    public static class AllTypes {
        private boolean booleanValue;
        private Boolean booleanObject;
        private int intValue;
        private Integer intObject;
        private double doubleValue;
        private Double doubleObject;
        private String string;
        private Person person;
        private Person definedPerson;

        public boolean isBooleanValue() {
            return booleanValue;
        }

        public void setBooleanValue(boolean booleanValue) {
            this.booleanValue = booleanValue;
        }

        public Boolean getBooleanObject() {
            return booleanObject;
        }

        public void setBooleanObject(Boolean booleanObject) {
            this.booleanObject = booleanObject;
        }

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        public Integer getIntObject() {
            return intObject;
        }

        public void setIntObject(Integer intObject) {
            this.intObject = intObject;
        }

        public double getDoubleValue() {
            return doubleValue;
        }

        public void setDoubleValue(double doubleValue) {
            this.doubleValue = doubleValue;
        }

        public Double getDoubleObject() {
            return doubleObject;
        }

        public void setDoubleObject(Double doubleObject) {
            this.doubleObject = doubleObject;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }

        public Person getDefinedPerson() {
            return definedPerson;
        }

        public void setDefinedPerson(Person definedPerson) {
            this.definedPerson = definedPerson;
        }

    }

    public static class Person {
        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

    }

    @Override
    protected Model getModel() {
        return (Model) super.getModel();
    }

    public TemplateModelDefaultValuesView() {
        getModel().setDefaultsInBean(new AllTypes());
        getModel().getDefaultsInBean().setDefinedPerson(new Person());
        getModel().setDefinedPerson(new Person());

    }

}
