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

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.ui.Grid;

/**
 * View for {@link Grid} demo.
 */
@ComponentDemo(name = "Grid", href = "vaadin-grid")
public class GridView extends DemoView {

    private Random random;

    /**
     * Example object.
     */
    public static class Person {
        private String name;
        private int age;

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

    }

    @Override
    void initView() {
        random = new Random();

        createBasicUsage();
        createCallBackDataProvider();
    }

    private void createBasicUsage() {
        // begin-source-example
        // source-example-heading: Grid Basics
        Grid<Person> grid = new Grid<>();
        grid.setItems(createItems());

        grid.addColumn("Name", Person::getName);
        grid.addColumn("Age", person -> Integer.toString(person.getAge()));

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
        grid.setDataProvider(DataProvider.fromCallbacks(query -> {
            return IntStream
                    .range(query.getOffset(),
                            query.getOffset() + query.getLimit())
                    .mapToObj(this::createPerson);
        }, query -> 10000));

        grid.addColumn("Name", Person::getName);
        grid.addColumn("Age", person -> Integer.toString(person.getAge()));

        // end-source-example

        grid.setId("lazy-loading");

        addCard("Grid with lazy loading", grid);
    }

    private Stream<Person> createItems() {
        return IntStream.range(1, 200).mapToObj(this::createPerson);
    }

    private Person createPerson(int index) {
        Person person = new Person();
        person.setName("Person " + index);
        person.setAge(13 + random.nextInt(50));
        return person;
    }
}
