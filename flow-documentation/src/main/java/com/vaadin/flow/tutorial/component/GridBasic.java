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

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.binder.Person;
import com.vaadin.ui.grid.Grid;
import com.vaadin.ui.layout.HorizontalLayout;

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
        grid.addColumn("Name", Person::getName);
        grid.addColumn("Year of birth",
                person -> Integer.toString(person.getYearOfBirth()));

        layout.add(grid);
    }
}
