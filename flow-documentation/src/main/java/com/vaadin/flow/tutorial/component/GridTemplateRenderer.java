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
package com.vaadin.flow.tutorial.component;

import java.time.Year;
import java.util.Arrays;
import java.util.List;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.grid.Grid;
import com.vaadin.ui.renderers.TemplateRenderer;

@CodeFor("flow-components/tutorial-flow-grid.asciidoc")
public class GridTemplateRenderer {

    public void basics() {
        List<Person> people = Arrays.asList(new Person(), new Person());

        Grid<Person> grid = new Grid<>();
        grid.setItems(people);

        grid.addColumn(TemplateRenderer.<Person> of("<b>[[item.name]]</b>")
                .withProperty("name", Person::getName)).setHeaderLabel("Name");
    }

    public void customProperties() {
        Grid<Person> grid = new Grid<>();

        grid.addColumn(TemplateRenderer.<Person> of("[[item.age]] years old")
                .withProperty("age",
                        person -> Year.now().getValue()
                                - person.getYearOfBirth()))
                .setHeaderLabel("Age");
    }

    public void bindingBeans() {
        Grid<Person> grid = new Grid<>();

        grid.addColumn(TemplateRenderer.<Person> of(
                "<div>[[item.address.street]], number [[item.address.number]]<br><small>[[item.address.postalCode]]</small></div>")
                .withProperty("address", Person::getAddress))
                .setHeaderLabel("Address");
    }

    public void handlingEvents() {
        Grid<Person> grid = new Grid<>();

        grid.addColumn(TemplateRenderer.<Person> of(
                "<button on-click='handleUpdate'>Update</button><button on-click='handleRemove'>Remove</button>")
                .withEventHandler("handleUpdate", person -> {
                    person.setName(person.getName() + " Updated");
                    grid.getDataProvider().refreshItem(person);
                }).withEventHandler("handleRemove", person -> {
                    ListDataProvider<Person> dataProvider = (ListDataProvider<Person>) grid
                            .getDataProvider();
                    dataProvider.getItems().remove(person);
                    dataProvider.refreshAll();
                })).setHeaderLabel("Actions");
    }

    public static class Person {
        private String name;
        private int yearOfBirth;
        private Address address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getYearOfBirth() {
            return yearOfBirth;
        }

        public void setYearOfBirth(int yearOfBirth) {
            this.yearOfBirth = yearOfBirth;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    public static class Address {
        private String street;
        private int number;
        private String postalCode;

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }
    }
}
