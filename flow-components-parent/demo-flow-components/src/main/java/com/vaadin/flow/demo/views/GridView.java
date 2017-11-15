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
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.checkbox.Checkbox;
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
import com.vaadin.ui.renderers.ComponentRenderer;
import com.vaadin.ui.renderers.TemplateRenderer;
import com.vaadin.ui.textfield.TextField;

/**
 * View for {@link Grid} demo.
 */
@Route(value = "vaadin-grid", layout = MainLayout.class)
@ComponentDemo(name = "Grid")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-grid.html")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-button.html")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-text-field.html")
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

    // begin-source-example
    // source-example-heading: Grid with columns using component renderer
    /**
     * Component used for the cell rendering.
     */
    public static class PersonComponent extends Div {

        private String text;
        private int timesClicked;

        /**
         * Zero-args constructor.
         */
        public PersonComponent() {
            this.addClickListener(event -> {
                timesClicked++;
                setText(text + "\nClicked " + timesClicked);
            });
        }

        /**
         * Sets the person for the component.
         * 
         * @param person
         *            the person to be inside inside the cell
         */
        public void setPerson(Person person) {
            text = "Hi, I'm " + person.getName() + "!";
            setText(text);
        }
    }

    /**
     * Component used for the details row.
     */
    public static class PersonCard extends Div {

        /**
         * Constructor that takes a Person as parameter.
         * 
         * @param person
         *            the person to be used inside the card
         */
        public PersonCard(Person person) {
            this.addClassName("custom-details");

            VerticalLayout layout1 = new VerticalLayout();
            layout1.add(new Label("Name: " + person.getName()));
            layout1.add(new Label("Id: " + person.getId()));
            layout1.add(new Label("Age: " + person.getAge()));

            VerticalLayout layout2 = new VerticalLayout();
            layout2.add(
                    new Label("Street: " + person.getAddress().getStreet()));
            layout2.add(new Label(
                    "Address number: " + person.getAddress().getNumber()));
            layout2.add(new Label(
                    "Postal Code: " + person.getAddress().getPostalCode()));

            HorizontalLayout hlayout = new HorizontalLayout(layout1, layout2);
            hlayout.getStyle().set("border", "1px solid gray")
                    .set("padding", "10px").set("boxSizing", "border-box")
                    .set("width", "100%");
            this.add(hlayout);
        }
    }
    // end-source-example

    @Override
    protected void initView() {
        createBasicUsage();
        createCallBackDataProvider();
        createSingleSelect();
        createMultiSelect();
        createNoneSelect();
        createColumnApiExample();
        createColumnTemplate();
        createDetailsRow();
        createColumnGroup();
        createColumnComponentRenderer();
        createSorting();
        createHeaderAndFooterUsingTemplates();
        createHeaderAndFooterUsingComponents();

        addCard("Grid example model",
                new Label("These objects are used in the examples above"));
    }

    private void createBasicUsage() {
        // begin-source-example
        // source-example-heading: Grid Basics
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        grid.addColumn(Person::getName).setHeaderLabel("Name");
        grid.addColumn(Person::getAge).setHeaderLabel("Age");

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

        grid.addColumn(Person::getName).setHeaderLabel("Name");
        grid.addColumn(Person::getAge).setHeaderLabel("Age");

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

        grid.addColumn(Person::getName).setHeaderLabel("Name");
        grid.addColumn(Person::getAge).setHeaderLabel("Age");

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
        addCard("Selection", "Grid Single Selection", grid, toggleSelect,
                messageDiv);
    }

    private void createMultiSelect() {
        Div messageDiv = new Div();
        // begin-source-example
        // source-example-heading: Grid Multi Selection
        List<Person> people = getItems();
        Grid<Person> grid = new Grid<>();
        grid.setItems(people);

        grid.addColumn(Person::getName).setHeaderLabel("Name");
        grid.addColumn(Person::getAge).setHeaderLabel("Age");

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
        addCard("Selection", "Grid Multi Selection", grid,
                new HorizontalLayout(selectBtn, selectAllBtn), messageDiv);
    }

    private void createNoneSelect() {
        // begin-source-example
        // source-example-heading: Grid with No Selection Enabled
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        grid.addColumn(Person::getName).setHeaderLabel("Name");
        grid.addColumn(Person::getAge).setHeaderLabel("Age");

        grid.setSelectionMode(SelectionMode.NONE);
        // end-source-example
        grid.setId("none-selection");
        addCard("Selection", "Grid with No Selection Enabled", grid);
    }

    private void createColumnTemplate() {
        // begin-source-example
        // source-example-heading: Grid with columns using template renderer
        Grid<Person> grid = new Grid<>();
        grid.setItems(createItems());

        // You can use the [[index]] variable to print the row index (0 based)
        grid.addColumn(TemplateRenderer.of("[[index]]")).setHeaderLabel("#");

        // You can set any property by using `withProperty`, including
        // properties not present on the original bean.
        grid.addColumn(TemplateRenderer.<Person> of(
                "<div title='[[item.name]]'>[[item.name]]<br><small>[[item.yearsOld]]</small></div>")
                .withProperty("name", Person::getName).withProperty("yearsOld",
                        person -> person.getAge() > 1
                                ? person.getAge() + " years old"
                                : person.getAge() + " year old"))
                .setHeaderLabel("Person");

        // You can also set complex objects directly. Internal properties of the
        // bean are accessible in the template.
        grid.addColumn(TemplateRenderer.<Person> of(
                "<div>[[item.address.street]], number [[item.address.number]]<br><small>[[item.address.postalCode]]</small></div>")
                .withProperty("address", Person::getAddress))
                .setHeaderLabel("Address");

        // You can set events handlers associated with the template. The syntax
        // follows the Polymer convention "on-event", such as "on-click".
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

        grid.setSelectionMode(SelectionMode.NONE);
        // end-source-example
        grid.setId("template-renderer");
        addCard("Using templates", "Grid with columns using template renderer",
                grid);
    }

    private void createColumnComponentRenderer() {
        // begin-source-example
        // source-example-heading: Grid with columns using component renderer
        Grid<Person> grid = new Grid<>();
        grid.setItems(createItems());

        // You can use a constructor and a separate setter for the renderer
        grid.addColumn(new ComponentRenderer<>(PersonComponent::new,
                PersonComponent::setPerson)).setHeaderLabel("Person");

        // Or you can use an ordinary function to get the component
        grid.addColumn(
                new ComponentRenderer<>(item -> new Button("Remove", evt -> {
                    ListDataProvider<Person> dataProvider = (ListDataProvider<Person>) grid
                            .getDataProvider();
                    dataProvider.getItems().remove(item);
                    grid.getDataCommunicator().reset();
                }))).setHeaderLabel("Actions");

        // Item details can also use components
        grid.setItemDetailsRenderer(new ComponentRenderer<>(PersonCard::new));

        // When items are updated, new components are generated
        TextField idField = new TextField("", "Person id");
        TextField nameField = new TextField("", "New name");

        Button updateButton = new Button("Update person", event -> {
            String id = idField.getValue();
            String name = nameField.getValue();
            ListDataProvider<Person> dataProvider = (ListDataProvider<Person>) grid
                    .getDataProvider();

            dataProvider.getItems().stream()
                    .filter(person -> String.valueOf(person.getId()).equals(id))
                    .findFirst().ifPresent(person -> {
                        person.setName(name);
                        dataProvider.refreshItem(person);
                    });

        });

        grid.setSelectionMode(SelectionMode.NONE);
        // end-source-example

        grid.setId("component-renderer");
        idField.setId("component-renderer-id-field");
        nameField.setId("component-renderer-name-field");
        updateButton.setId("component-renderer-update-button");
        addCard("Using components",
                "Grid with columns using component renderer", grid, idField,
                nameField, updateButton);
    }

    private void createColumnApiExample() {
        // begin-source-example
        // source-example-heading: Column API example
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        Column<Person> idColumn = grid.addColumn(Person::getId)
                .setHeaderLabel("ID").setFlexGrow(0).setWidth("75px");
        Column<Person> nameColumn = grid.addColumn(Person::getName)
                .setHeaderLabel("Name").setResizable(true);
        grid.addColumn(Person::getAge).setHeaderLabel("Age").setResizable(true);

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
            grid.mergeColumns(idColumn, nameColumn)
                    .setHeaderLabel("ID, Name column group");
            // Remove this button from the layout
            merge.getParent().ifPresent(
                    component -> ((HasComponents) component).remove(merge));
        });
        // end-source-example

        grid.setId("column-api-example");
        idColumnVisibility.setId("toggle-id-column-visibility");
        userReordering.setId("toggle-user-reordering");
        freezeIdColumn.setId("toggle-id-column-frozen");
        addCard("Configuring columns", "Column API example", grid,
                new VerticalLayout(idColumnVisibility, userReordering,
                        freezeIdColumn, merge));
    }

    private void createDetailsRow() {
        // begin-source-example
        // source-example-heading: Grid with a details row
        Grid<Person> grid = new Grid<>();
        List<Person> people = createItems();
        grid.setItems(people);

        grid.addColumn(Person::getName).setHeaderLabel("Name");
        grid.addColumn(Person::getAge).setHeaderLabel("Age");

        grid.setSelectionMode(SelectionMode.NONE);
        grid.setItemDetailsRenderer(TemplateRenderer
                .<Person> of("<div class='custom-details'>"
                        + "<div>Hi! My name is [[item.name]]!</div>"
                        + "<div><vaadin-button on-click='handleClick'>Update Person</vaadin-button></div>"
                        + "</div>")
                .withProperty("name", Person::getName)
                .withEventHandler("handleClick", person -> {
                    person.setName(person.getName() + " Updated");
                    grid.getDataProvider().refreshItem(person);
                }));

        Button toggleDetails = new Button("Toggle details open for second row");
        toggleDetails
                .addClickListener(event -> grid.setDetailsVisible(people.get(1),
                        !grid.isDetailsVisible(people.get(1))));
        // end-source-example
        grid.setId("grid-with-details-row");
        toggleDetails.setId("toggle-details-button");
        addCard("Using templates", "Grid with a details row", grid,
                toggleDetails);
    }

    private void createColumnGroup() {
        // begin-source-example
        // source-example-heading: Column grouping example
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        Column<Person> nameColumn = grid.addColumn(Person::getName)
                .setHeaderLabel("Name");
        Column<Person> ageColumn = grid.addColumn(Person::getAge)
                .setHeaderLabel("Age");
        Column<Person> streetColumn = grid
                .addColumn(person -> person.getAddress().getStreet())
                .setHeaderLabel("Street");
        Column<Person> postalCodeColumn = grid
                .addColumn(person -> person.getAddress().getPostalCode())
                .setHeaderLabel("Postal Code");

        ColumnGroup<Person> informationColumnGroup = grid
                .mergeColumns(nameColumn, ageColumn)
                .setHeaderLabel("Basic Information")
                .setFooterLabel("Total: " + getItems().size() + " people");
        ColumnGroup<Person> addressColumnGroup = grid
                .mergeColumns(streetColumn, postalCodeColumn)
                .setHeaderLabel("Address Information");
        grid.mergeColumns(informationColumnGroup, addressColumnGroup);
        // end-source-example
        grid.setId("grid-column-grouping");
        addCard("Configuring columns", "Column grouping example", grid);
    }

    private void createSorting() {
        Div messageDiv = new Div();
        // begin-source-example
        // source-example-heading: Grid with sortable columns
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());
        grid.setSelectionMode(SelectionMode.NONE);

        grid.addColumn(Person::getName, "name").setHeaderLabel("Name");
        grid.addColumn(Person::getAge, "age").setHeaderLabel("Age");

        grid.addColumn(TemplateRenderer.<Person> of(
                "<div>[[item.street]], number [[item.number]]<br><small>[[item.postalCode]]</small></div>")
                .withProperty("street",
                        person -> person.getAddress().getStreet())
                .withProperty("number",
                        person -> person.getAddress().getNumber())
                .withProperty("postalCode",
                        person -> person.getAddress().getPostalCode()),
                "street", "number").setHeaderLabel("Address");

        Checkbox multiSort = new Checkbox("Multiple column sorting enabled");
        multiSort.addValueChangeListener(
                event -> grid.setMultiSort(event.getValue()));
        grid.addSortListener(event -> {
            String currentSortOrder = grid.getDataCommunicator()
                    .getBackEndSorting().stream()
                    .map(querySortOrder -> String.format(
                            "{sort property: %s, direction: %s}",
                            querySortOrder.getSorted(),
                            querySortOrder.getDirection()))
                    .collect(Collectors.joining(", "));
            messageDiv.setText(String.format(
                    "Current sort order: %s. Sort originates from the client: %s.",
                    currentSortOrder, event.isFromClient()));
        });
        // end-source-example
        grid.setId("grid-sortable-columns");
        multiSort.setId("grid-multi-sort-toggle");
        messageDiv.setId("grid-sortable-columns-message");
        addCard("Sorting", "Grid with sortable columns", grid, multiSort,
                messageDiv);
    }

    private void createHeaderAndFooterUsingTemplates() {
        // begin-source-example
        // source-example-heading: Column header and footer using templates
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        Column<Person> nameColumn = grid.addColumn(Person::getName)
                .setHeaderLabel(TemplateRenderer.of(
                        "<span style='color:green' title='Name'>Name</span>"))
                .setComparator((p1, p2) -> p1.getName()
                        .compareToIgnoreCase(p2.getName()));
        Column<Person> ageColumn = grid.addColumn(Person::getAge, "age")
                .setHeaderLabel(TemplateRenderer
                        .of("<span style='color:blue' title='Age'>Age</span>"));
        Column<Person> streetColumn = grid
                .addColumn(person -> person.getAddress().getStreet())
                .setHeaderLabel("Street");
        Column<Person> postalCodeColumn = grid
                .addColumn(person -> person.getAddress().getPostalCode())
                .setHeaderLabel("Postal Code");

        ColumnGroup<Person> informationColumnGroup = grid
                .mergeColumns(nameColumn, ageColumn)
                .setHeaderLabel(TemplateRenderer.of(
                        "<span style='color:orange' title='Basic Information'>Basic Information</span>"))
                .setFooterLabel(
                        TemplateRenderer.of("<span style='color:red'>Total: "
                                + getItems().size() + " people</span>"));
        ColumnGroup<Person> addressColumnGroup = grid
                .mergeColumns(streetColumn, postalCodeColumn)
                .setHeaderLabel(TemplateRenderer.of(
                        "<span title='Address Information'>Address Information</span>"));
        grid.mergeColumns(informationColumnGroup, addressColumnGroup);

        // end-source-example
        grid.setId("grid-header-with-templates");
        addCard("Using templates", "Column header and footer using templates",
                grid);
    }

    private void createHeaderAndFooterUsingComponents() {
        // begin-source-example
        // source-example-heading: Column header and footer using components
        Grid<Person> grid = new Grid<>();
        grid.setItems(getItems());

        Column<Person> nameColumn = grid.addColumn(Person::getName)
                .setHeaderLabel(new ComponentRenderer<>(() -> {
                    Label label = new Label("Name");
                    label.getStyle().set("color", "green");
                    label.setTitle("Name");
                    return label;
                })).setComparator((p1, p2) -> p1.getName()
                        .compareToIgnoreCase(p2.getName()));
        Column<Person> ageColumn = grid.addColumn(Person::getAge, "age")
                .setHeaderLabel(new ComponentRenderer<>(() -> {
                    Label label = new Label("Age");
                    label.getStyle().set("color", "blue");
                    label.setTitle("Age");
                    return label;
                }));
        Column<Person> streetColumn = grid
                .addColumn(person -> person.getAddress().getStreet())
                .setHeaderLabel("Street");
        Column<Person> postalCodeColumn = grid
                .addColumn(person -> person.getAddress().getPostalCode())
                .setHeaderLabel("Postal Code");

        ColumnGroup<Person> informationColumnGroup = grid
                .mergeColumns(nameColumn, ageColumn)
                .setHeaderLabel(new ComponentRenderer<>(() -> {
                    Label label = new Label("Basic Information");
                    label.getStyle().set("color", "orange");
                    label.setTitle("Basic Information");
                    return label;
                })).setFooterLabel(new ComponentRenderer<>(() -> {
                    Label label = new Label(
                            "Total: " + getItems().size() + " people");
                    label.getStyle().set("color", "red");
                    return label;
                }));
        ColumnGroup<Person> addressColumnGroup = grid
                .mergeColumns(streetColumn, postalCodeColumn)
                .setHeaderLabel(new ComponentRenderer<>(() -> {
                    Label label = new Label("Address Information");
                    label.setTitle("Address Information");
                    return label;
                }));
        grid.mergeColumns(informationColumnGroup, addressColumnGroup);

        // end-source-example
        Checkbox toggleSortable = new Checkbox("Toggle sorting for the Grid",
                event -> grid.getColumns().forEach(
                        column -> column.setSortable(event.getValue())));
        toggleSortable.setValue(true);

        grid.setId("grid-header-with-components");
        addCard("Using components", "Column header and footer using components",
                grid, new HorizontalLayout(toggleSortable));
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
