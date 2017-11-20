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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.binder.Person;
import com.vaadin.function.ValueProvider;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.grid.ColumnGroup;
import com.vaadin.ui.grid.Grid;
import com.vaadin.ui.grid.Grid.Column;
import com.vaadin.ui.grid.Grid.SelectionMode;
import com.vaadin.ui.grid.GridMultiSelectionModel;
import com.vaadin.ui.grid.GridSingleSelectionModel;
import com.vaadin.ui.html.Label;
import com.vaadin.ui.layout.HorizontalLayout;
import com.vaadin.ui.renderers.TemplateRenderer;

@CodeFor("flow-components/tutorial-flow-grid.asciidoc")
public class GridBasic {

    public void basics() {
        HorizontalLayout layout = new HorizontalLayout();

        // Have some data
        List<Person> people = Arrays.asList(
                new Person("Nicolaus Copernicus", 1543),
                new Person("Galileo Galilei", 1564),
                new Person("Johannes Kepler", 1571));

        // Create a grid bound to the list
        Grid<Person> grid = new Grid<>();
        grid.setItems(people);
        grid.addColumn(Person::getName).setHeader("Name");
        grid.addColumn(person -> Integer.toString(person.getYearOfBirth()))
                .setHeader("Year of birth");

        layout.add(grid);
    }

    public void handlingSelection() {
        Label message = new Label();
        Grid<Person> grid = new Grid<>();

        // switch to multiselect mode
        grid.setSelectionMode(SelectionMode.MULTI);

        grid.addSelectionListener(event -> {
            Set<Person> selected = event.getAllSelectedItems();
            message.setText(selected.size() + " items selected");
        });

        Person defaultItem = null;
        List<Person> people = null;

        // in single-select, only one item is selected
        grid.select(defaultItem);

        // switch to multi select, clears selection
        grid.setSelectionMode(SelectionMode.MULTI);
        // Select items 2-4
        people.subList(2, 3).forEach(grid::select);

        // the default selection model
        GridSingleSelectionModel<Person> defaultModel = (GridSingleSelectionModel<Person>) grid
                .getSelectionModel();

        // Use multi-selection mode
        GridMultiSelectionModel<Person> selectionModel = (GridMultiSelectionModel<Person>) grid
                .setSelectionMode(SelectionMode.MULTI);

        // preselect value
        grid.select(defaultItem);

        GridSingleSelectionModel<Person> singleSelect = (GridSingleSelectionModel<Person>) grid
                .getSelectionModel();
        // disallow empty selection
        singleSelect.setDeselectAllowed(false);
    }

    public void gridMultiSelect() {
        List<Person> people = null;
        Label message = new Label();
        Button deleteSelected = null;

        // Grid in multi-selection mode
        Grid<Person> grid = new Grid<>();
        grid.setItems(people);
        GridMultiSelectionModel<Person> selectionModel = (GridMultiSelectionModel<Person>) grid
                .setSelectionMode(SelectionMode.MULTI);

        selectionModel.selectAll();

        selectionModel.addMultiSelectionListener(event -> {
            message.setText(String.format("%s items added, %s removed.",
                    event.getAddedSelection().size(),
                    event.getRemovedSelection().size()));

            // Allow deleting only if there's any selected
            deleteSelected.setDisabled(event.getNewSelection().isEmpty());
        });
    }

    public void gridConfiguringColumns() {
        Grid<Person> grid = new Grid<>();

        //@formatter:off
        Column<Person> nameColumn = grid.addColumn(Person::getName)
                .setHeader("Name")
                .setFlexGrow(0)
                .setWidth("100px")
                .setResizable(false);
        //@formatter:on

        grid.setColumnReorderingAllowed(true);

        nameColumn.setFrozen(true);
    }

    public void gridColumnMerging() {
        Grid<Person> grid = new Grid<>();
        Column<Person> nameColumn = grid.addColumn(ValueProvider.identity());
        Column<Person> ageColumn = grid.addColumn(ValueProvider.identity());
        Column<Person> streetColumn = grid.addColumn(ValueProvider.identity());
        Column<Person> postalCodeColumn = grid
                .addColumn(ValueProvider.identity());

        // Group two columns, nameColumn and ageColumn,
        // in a ColumnGroup and set the header text
        ColumnGroup informationColumnGroup = grid
                .mergeColumns(nameColumn, ageColumn)
                .setHeader("Basic Information");

        ColumnGroup addressColumnGroup = grid
                .mergeColumns(streetColumn, postalCodeColumn)
                .setHeader("Address information");

        // Group two ColumnGroups
        grid.mergeColumns(informationColumnGroup, addressColumnGroup)
                .setHeader("Person Information");

    }

    public void gridHeadersAndFooters() {
        Grid<Person> grid = new Grid<>();
        Column<Person> nameColumn = grid.addColumn(ValueProvider.identity());

        // Sets a simple text header
        nameColumn.setHeader("Name");
        // Sets a header containing a custom template,
        // in this case simply bolding the caption "Name"
        nameColumn.setHeader(TemplateRenderer.<Person> of("<b>Name</b>"));

        // Similarly for the footer
        nameColumn.setFooter("Name");
        nameColumn.setFooter(TemplateRenderer.<Person> of("<b>Name</b>"));
    }

    public void gridSorting() {
        Grid<Person> grid = new Grid<>();

        grid.setMultiSort(true);

        grid.addColumn(Person::getAge, "age").setHeader("Age");

        grid.addColumn(person -> person.getName() + " " + person.getLastName(),
                "name", "lastName").setHeader("Name");

        grid.addColumn(TemplateRenderer.<Person> of(
                "<div>[[item.name]]<br><small>[[item.email]]</small></div>")
                .withProperty("name", Person::getName)
                .withProperty("email", Person::getEmail), "name", "email")
                .setHeader("Person");

        grid.addColumn(Person::getName)
                .setComparator((person1, person2) -> person1.getName()
                        .compareToIgnoreCase(person2.getName()))
                .setHeader("Name");

        grid.addSortListener(event -> {
            String currentSortOrder = grid.getDataCommunicator()
                    .getBackEndSorting().stream()
                    .map(querySortOrder -> String.format(
                            "{sort property: %s, direction: %s}",
                            querySortOrder.getSorted(),
                            querySortOrder.getDirection()))
                    .collect(Collectors.joining(", "));
            System.out.println(String.format(
                    "Current sort order: %s. Sort originates from the client: %s.",
                    currentSortOrder, event.isFromClient()));
        });
    }

    //@formatter:off
    /*
     * code of commented lines
     * 
     grid.setColumnOrder(firstnameColumn, lastnameColumn,
                    bornColumn, birthplaceColumn,
                    diedColumn);
     */
    //@formatter:on
}
