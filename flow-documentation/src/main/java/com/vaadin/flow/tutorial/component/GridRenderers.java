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

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcons;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.renderer.ButtonRenderer;
import com.vaadin.flow.renderer.ComponentTemplateRenderer;
import com.vaadin.flow.renderer.LocalDateRenderer;
import com.vaadin.flow.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.renderer.NumberRenderer;
import com.vaadin.flow.renderer.TemplateRenderer;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.binder.Person.Gender;

@CodeFor("flow-components/tutorial-flow-grid.asciidoc")
public class GridRenderers {

    private List<Person> people = Arrays.asList(new Person(), new Person());

    public void basicRenderers() {
        Grid<Item> grid = new Grid<>();

        grid.addColumn(new LocalDateRenderer<>(Item::getEstimatedDeliveryDate,
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                .setHeader("Estimated delivery date");

        grid.addColumn(new LocalDateRenderer<>(Item::getEstimatedDeliveryDate,
                "dd/MM/yyyy")).setHeader("Estimated delivery date");

        grid.addColumn(new LocalDateTimeRenderer<>(Item::getPurchaseDate,
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT,
                        FormatStyle.MEDIUM)))
                .setHeader("Purchase date and time");

        grid.addColumn(new LocalDateTimeRenderer<>(Item::getPurchaseDate,
                "dd/MM HH:mm:ss")).setHeader("Purchase date and time");

        grid.addColumn(new NumberRenderer<>(Item::getPrice,
                NumberFormat.getCurrencyInstance())).setHeader("Price");

        grid.addColumn(new NumberRenderer<>(Item::getPrice, "$ %(,.2f",
                Locale.US, "$ 0.00")).setHeader("Price");

        grid.addColumn(new ButtonRenderer<>("Remove item", clickedItem -> {
            // remove the item
        }));

        //@formatter:off
        grid.addColumn(new ButtonRenderer<>(item -> "Remove " + item, clickedItem -> {
            // remove the item
        }));
        //@formatter:on
    }

    public void templateRenderer() {
        Grid<Person> grid = new Grid<>();
        grid.setItems(people);

        grid.addColumn(TemplateRenderer.<Person> of("<b>[[item.name]]</b>")
                .withProperty("name", Person::getName)).setHeader("Name");

        //@formatter:off
        grid.addColumn(TemplateRenderer.<Person> of("[[item.age]] years old")
                        .withProperty("age",
                                person -> Year.now().getValue()
                                        - person.getYearOfBirth()))
                .setHeader("Age");
        //@formatter:on

        grid.addColumn(TemplateRenderer.<Person> of(
                "<div>[[item.address.street]], number [[item.address.number]]<br><small>[[item.address.postalCode]]</small></div>")
                .withProperty("address", Person::getAddress))
                .setHeader("Address");

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
                })).setHeader("Actions");
    }

    public void componentRenderer() {
        Grid<Person> grid = new Grid<>();
        grid.setItems(people);

        grid.addColumn(new ComponentTemplateRenderer<>(person -> {
            if (person.getGender() == Gender.MALE) {
                return new Icon(VaadinIcons.MALE);
            } else {
                return new Icon(VaadinIcons.FEMALE);
            }
        })).setHeader("Gender");

        grid.addColumn(new ComponentTemplateRenderer<>(Div::new,
                (div, person) -> div.setText(person.getName())))
                .setHeader("Name");

        grid.addColumn(new ComponentTemplateRenderer<>(
                () -> new Icon(VaadinIcons.ARROW_LEFT)));

        grid.addColumn(new ComponentTemplateRenderer<>(person -> {

            // text field for entering a new name for the person
            TextField name = new TextField("Name");
            name.setValue(person.getName());

            // button for saving the name to backend
            Button update = new Button("Update", event -> {
                person.setName(name.getValue());
                grid.getDataProvider().refreshItem(person);
            });

            // button that removes the item
            Button remove = new Button("Remove", event -> {
                ListDataProvider<Person> dataProvider = (ListDataProvider<Person>) grid
                        .getDataProvider();
                dataProvider.getItems().remove(person);
                dataProvider.refreshAll();
            });

            // layouts for placing the text field on top of the buttons
            HorizontalLayout buttons = new HorizontalLayout(update, remove);
            return new VerticalLayout(name, buttons);
        })).setHeader("Actions");
    }

    public void showingItemDetails() {
        Grid<Person> grid = new Grid<>();

        grid.setItemDetailsRenderer(new ComponentTemplateRenderer<>(person -> {
            VerticalLayout layout = new VerticalLayout();
            layout.add(new Label("Address: " + person.getAddress().getStreet()
                    + " " + person.getAddress().getNumber()));
            layout.add(new Label("Year of birth: " + person.getYearOfBirth()));
            return layout;
        }));
    }

    public static class Person {
        private String name;
        private int yearOfBirth;
        private Address address;
        private Gender gender;

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

        public Gender getGender() {
            return gender;
        }

        public void setGender(Gender gender) {
            this.gender = gender;
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

    public static class Item {
        private String name;
        private double price;
        private LocalDateTime purchaseDate;
        private LocalDate estimatedDeliveryDate;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public LocalDateTime getPurchaseDate() {
            return purchaseDate;
        }

        public void setPurchaseDate(LocalDateTime purchaseDate) {
            this.purchaseDate = purchaseDate;
        }

        public LocalDate getEstimatedDeliveryDate() {
            return estimatedDeliveryDate;
        }

        public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
            this.estimatedDeliveryDate = estimatedDeliveryDate;
        }
    }
}
