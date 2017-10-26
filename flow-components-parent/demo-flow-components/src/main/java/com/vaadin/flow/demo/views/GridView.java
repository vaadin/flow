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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.common.HasComponents;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.grid.ColumnGroup;
import com.vaadin.ui.grid.Grid;
import com.vaadin.ui.grid.Grid.Column;
import com.vaadin.ui.grid.Grid.SelectionMode;
import com.vaadin.ui.grid.GridMultiSelectionModel;
import com.vaadin.ui.grid.GridSelectionModel;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.html.Label;
import com.vaadin.ui.layout.HorizontalLayout;
import com.vaadin.ui.layout.VerticalLayout;
import com.vaadin.ui.renderers.TemplateRenderer;

/**
 * View for {@link Grid} demo.
 */
@ComponentDemo(name = "Grid", href = "vaadin-grid")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-grid.html")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-button.html")
public class GridView extends DemoView {

    static List<Person> items = new ArrayList<>();
    static {
        items = createItems();
    }

    // begin-source-example
    // source-example-heading: Grid example model
    /**
     * Example object.
     */
    public static class Person {
        private int id;
        private String name;
        private int age;
        private Address address;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Person)) {
                return false;
            }
            Person other = (Person) obj;
            return id == other.id;
        }

        @Override
        public String toString() {
            return String.format("Person [name=%s, age=%s]", name, age);
        }
    }

    /**
     * Example object.
     */
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
    // end-source-example

    @Override
    void initView() {
        createBasicUsage();
        createCallBackDataProvider();
        createSingleSelect();
        createMultiSelect();
        createNoneSelect();
        createColumnTemplate();
        createColumnApiExample();
        createDetailsRow();
        createColumnGroup();

        addCard("Grid example model",
                new Label("These objects are used in the examples above"));
    }

    private void createBasicUsage() {
        // begin-source-example
        // source-example-heading: Grid Basics
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        grid.addColumn("Name", Person::getName);
        grid.addColumn("Age", Person::getAge);

        // end-source-example
        grid.setId("basic");

        addCard("Grid Basics", grid);
    }

    private void createCallBackDataProvider() {
        // begin-source-example
        // source-example-heading: Grid with lazy loading
        Grid<Person> grid = new Grid<>();

        /*
         * This Data Provider doesn't load all items into the memory right away.
         * Grid will request only the data that should be shown in its current
         * view "window". The Data Provider will use callbacks to load only a
         * portion of the data.
         */
        Random random = new Random(0);
        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> IntStream
                        .range(query.getOffset(),
                                query.getOffset() + query.getLimit())
                        .mapToObj(index -> createPerson(index, random)),
                query -> 10000));

        grid.addColumn("Name", Person::getName);
        grid.addColumn("Age", Person::getAge);

        // end-source-example

        grid.setId("lazy-loading");

        addCard("Grid with lazy loading", grid);
    }

    private void createSingleSelect() {
        Div messageDiv = new Div();
        // begin-source-example
        // source-example-heading: Grid Single Selection
        List<Person> people = getItems();
        Grid<Person> grid = new Grid<>();
        grid.setItems(people);

        grid.addColumn("Name", Person::getName);
        grid.addColumn("Age", Person::getAge);

        grid.asSingleSelect().addValueChangeListener(
                event -> messageDiv.setText(String.format(
                        "Selection changed from %s to %s, selection is from client: %s",
                        event.getOldValue(), event.getValue(),
                        event.isFromClient())));

        Button toggleSelect = new Button(
                "Toggle selection of the first person");
        Person firstPerson = people.get(0);
        toggleSelect.addClickListener(event -> {
            GridSelectionModel<Person> selectionModel = grid
                    .getSelectionModel();
            if (selectionModel.isSelected(firstPerson)) {
                selectionModel.deselect(firstPerson);
            } else {
                selectionModel.select(firstPerson);
            }
        });
        // end-source-example
        grid.setId("single-selection");
        toggleSelect.setId("single-selection-toggle");
        messageDiv.setId("single-selection-message");
        addCard("Grid Single Selection", grid, toggleSelect, messageDiv);
    }

    private void createMultiSelect() {
        Div messageDiv = new Div();
        // begin-source-example
        // source-example-heading: Grid Multi Selection
        List<Person> people = getItems();
        Grid<Person> grid = new Grid<>();
        grid.setItems(people);

        grid.addColumn("Name", Person::getName);
        grid.addColumn("Age", Person::getAge);

        grid.setSelectionMode(SelectionMode.MULTI);

        grid.asMultiSelect().addValueChangeListener(
                event -> messageDiv.setText(String.format(
                        "Selection changed from %s to %s, selection is from client: %s",
                        event.getOldValue(), event.getValue(),
                        event.isFromClient())));

        Button selectBtn = new Button("Select first five persons");
        selectBtn.addClickListener(event -> grid.asMultiSelect()
                .setValue(new LinkedHashSet<>(people.subList(0, 5))));
        Button selectAllBtn = new Button("Select all");
        selectAllBtn.addClickListener(
                event -> ((GridMultiSelectionModel<Person>) grid
                        .getSelectionModel()).selectAll());
        // end-source-example
        grid.setId("multi-selection");
        selectBtn.setId("multi-selection-button");
        messageDiv.setId("multi-selection-message");
        addCard("Grid Multi Selection", grid,
                new HorizontalLayout(selectBtn, selectAllBtn), messageDiv);
    }

    private void createNoneSelect() {
        // begin-source-example
        // source-example-heading: Grid with No Selection Enabled
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        grid.addColumn("Name", Person::getName);
        grid.addColumn("Age", Person::getAge);

        grid.setSelectionMode(SelectionMode.NONE);
        // end-source-example
        grid.setId("none-selection");
        addCard("Grid with No Selection Enabled", grid);
    }

    private void createColumnTemplate() {
        // begin-source-example
        // source-example-heading: Grid with columns using template renderer
        Grid<Person> grid = new Grid<>();
        grid.setItems(createItems());

        // You can use the [[index]] variable to print the row index (0 based)
        grid.addColumn("#", TemplateRenderer.of("[[index]]"));

        // You can set any property by using `withProperty`, including
        // properties not present on the original bean.
        grid.addColumn("Person", TemplateRenderer.<Person> of(
                "<div title='[[item.name]]'>[[item.name]]<br><small>[[item.yearsOld]]</small></div>")
                .withProperty("name", Person::getName).withProperty("yearsOld",
                        person -> person.getAge() > 1
                                ? person.getAge() + " years old"
                                : person.getAge() + " year old"));

        // You can also set complex objects directly. Internal properties of the
        // bean are accessible in the template.
        grid.addColumn("Address", TemplateRenderer.<Person> of(
                "<div>[[item.address.street]], number [[item.address.number]]<br><small>[[item.address.postalCode]]</small></div>")
                .withProperty("address", Person::getAddress));

        // You can set events handlers associated with the template. The syntax
        // follows the Polymer convention "on-event", such as "on-click".
        grid.addColumn("Actions", TemplateRenderer.<Person> of(
                "<button on-click='handleUpdate'>Update</button><button on-click='handleRemove'>Remove</button>")
                .withEventHandler("handleUpdate", person -> {
                    person.setName(person.getName() + " Updated");
                    grid.getDataCommunicator().reset();
                }).withEventHandler("handleRemove", person -> {
                    ListDataProvider<Person> dataProvider = (ListDataProvider<Person>) grid
                            .getDataCommunicator().getDataProvider();
                    dataProvider.getItems().remove(person);
                    grid.getDataCommunicator().reset();
                }));

        grid.setSelectionMode(SelectionMode.NONE);
        // end-source-example
        grid.setId("template-renderer");
        addCard("Grid with columns using template renderer", grid);
    }

    private void createColumnApiExample() {
        // begin-source-example
        // source-example-heading: Column API example
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        Column<Person> idColumn = grid.addColumn("ID", Person::getId)
                .setFlexGrow(0).setWidth("75px");
        Column<Person> nameColumn = grid.addColumn("Name", Person::getName)
                .setResizable(true);
        grid.addColumn("Age", Person::getAge).setResizable(true);

        Button idColumnVisibility = new Button(
                "Toggle visibility of the ID column");
        idColumnVisibility.addClickListener(
                event -> idColumn.setHidden(!idColumn.isHidden()));

        Button userReordering = new Button("Toggle user reordering of columns");
        userReordering.addClickListener(event -> grid
                .setColumnReorderingAllowed(!grid.isColumnReorderingAllowed()));

        Button freezeIdColumn = new Button("Toggle frozen state of ID column");
        freezeIdColumn.addClickListener(
                event -> idColumn.setFrozen(!idColumn.isFrozen()));

        Button merge = new Button("Merge ID and name columns");
        merge.addClickListener(event -> {
            grid.mergeColumns("ID, Name column group", idColumn, nameColumn);
            // Remove this button from the layout
            merge.getParent().ifPresent(
                    component -> ((HasComponents) component).remove(merge));
        });
        // end-source-example

        grid.setId("column-api-example");
        idColumnVisibility.setId("toggle-id-column-visibility");
        userReordering.setId("toggle-user-reordering");
        freezeIdColumn.setId("toggle-id-column-frozen");
        addCard("Column API example", grid, new VerticalLayout(
                idColumnVisibility, userReordering, freezeIdColumn, merge));
    }

    private void createDetailsRow() {
        // begin-source-example
        // source-example-heading: Grid with a details row
        Grid<Person> grid = new Grid<>();
        grid.setItems(createItems());

        grid.addColumn("Name", Person::getName);
        grid.addColumn("Age", Person::getAge);

        grid.setSelectionMode(SelectionMode.NONE);
        grid.setItemDetailsRenderer(TemplateRenderer
                .<Person> of("<div class='custom-details'>"
                        + "<div>Hi! My name is [[item.name]]!</div>"
                        + "<div><vaadin-button on-click='handleClick'>Update Person</vaadin-button></div>"
                        + "</div>")
                .withProperty("name", Person::getName)
                .withEventHandler("handleClick", person -> {
                    person.setName(person.getName() + " Updated");
                    grid.getDataCommunicator().refresh(person);
                }));
        // end-source-example
        grid.setId("grid-with-details-row");
        addCard("Grid with a details row", grid);
    }

    private void createColumnGroup() {
        // begin-source-example
        // source-example-heading: Column grouping example
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        Column<Person> nameColumn = grid.addColumn("Name", Person::getName);
        Column<Person> ageColumn = grid.addColumn("Age", Person::getAge);
        Column<Person> streetColumn = grid.addColumn("Street",
                person -> person.getAddress().getStreet());
        Column<Person> postalCodeColumn = grid.addColumn("Postal Code",
                person -> person.getAddress().getPostalCode());

        ColumnGroup informationColumnGroup = grid
                .mergeColumns("Basic Information", nameColumn, ageColumn);
        ColumnGroup addressColumnGroup = grid.mergeColumns(
                "Address information", streetColumn, postalCodeColumn);
        grid.mergeColumns("Person Information", informationColumnGroup,
                addressColumnGroup);
        // end-source-example
        grid.setId("grid-column-grouping");
        addCard("Column grouping example", grid);
    }

    private List<Person> getItems() {
        return items;
    }

    private static List<Person> createItems() {
        Random random = new Random(0);
        return IntStream.range(1, 500)
                .mapToObj(index -> createPerson(index, random))
                .collect(Collectors.toList());
    }

    private static Person createPerson(int index, Random random) {
        Person person = new Person();
        person.setId(index);
        person.setName("Person " + index);
        person.setAge(13 + random.nextInt(50));

        Address address = new Address();
        address.setStreet("Street " + ((char) ('A' + random.nextInt(26))));
        address.setNumber(1 + random.nextInt(50));
        address.setPostalCode(String.valueOf(10000 + random.nextInt(8999)));
        person.setAddress(address);

        return person;
    }
}
