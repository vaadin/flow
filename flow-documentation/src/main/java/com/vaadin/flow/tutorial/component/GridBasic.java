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
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.binder.Person;
import com.vaadin.ui.button.Button;
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
        grid.addColumn(Person::getName).setHeaderLabel("Name");
        grid.addColumn(person -> Integer.toString(person.getYearOfBirth()))
                .setHeaderLabel("Year of birth");

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
        GridSingleSelectionModel<Person> defaultModel = 
                (GridSingleSelectionModel<Person>) grid.getSelectionModel();

        // Use multi-selection mode
        GridMultiSelectionModel<Person> selectionModel = 
                (GridMultiSelectionModel<Person>) grid.setSelectionMode(SelectionMode.MULTI);

        // preselect value
        grid.select(defaultItem);

        GridSingleSelectionModel<Person> singleSelect =
                (GridSingleSelectionModel<Person>) grid.getSelectionModel();
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
        GridMultiSelectionModel<Person> selectionModel =
                (GridMultiSelectionModel<Person>) grid.setSelectionMode(SelectionMode.MULTI);

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
        
        Column<Person> nameColumn = grid.addColumn(Person::getName)
            .setHeaderLabel("Name")
            .setFlexGrow(0)
            .setWidth("100px")
            .setResizable(false);
        
        grid.setColumnReorderingAllowed(true);
        
        nameColumn.setFrozen(true);
    }
    
    public void gridHeaderAndFooter() {
        Grid<Person> grid = new Grid<>();
        
        Column<Person> bornColumn = grid.addColumn(Person::getYearOfBirth);
        bornColumn.setHeaderLabel("Born date");
        
        bornColumn.setFooterLabel("Summary");
    }
    
    /* code of commented lines

     grid.setColumnOrder(firstnameColumn, lastnameColumn,
                    bornColumn, birthplaceColumn,
                    diedColumn);
     */
}
