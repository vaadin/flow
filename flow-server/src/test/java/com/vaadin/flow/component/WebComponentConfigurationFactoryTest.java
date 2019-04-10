/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.webcomponent.WebComponent;

public class WebComponentConfigurationFactoryTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private WebComponentExporter.WebComponentConfigurationFactory factory =
            new WebComponentExporter.WebComponentConfigurationFactory();


    @Test
    public void create_class_throwsOnNullArgument() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("'clazz'");

        factory.create((Class<? extends WebComponentExporter<?
                extends Component>>) null);
    }

    @Test
    public void create_instance_throwsOnNullArgument() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("'exporter'");

        factory.create(( WebComponentExporter<?
                extends Component>) null);
    }

    @Test
    public void create_class_throwsOnNullTag() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Unable to construct " +
                "WebComponentConfiguration! Did 'com.vaadin.flow.component" +
                ".WebComponentConfigurationFactoryTest.NullTagExporter' give " +
                "null value to super(String) constructor?");

        factory.create(NullTagExporter.class);
    }

    @Test
    public void create_class_throwsOnMissingDefaultConstructor() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(String.format("Unable to create an instance " +
                "of '%s'. Make sure the class has a public no-arg constructor.",
                NoDefaultConstructorExporter.class.getName()));

        factory.create(NoDefaultConstructorExporter.class);
    }

    public static class MyComponent extends Component {

    }

    public static class NullTagExporter
            extends WebComponentExporter<MyComponent> {

        public NullTagExporter() {
            super(null);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent, MyComponent component) {

        }
    }

    public static class NoDefaultConstructorExporter extends WebComponentExporter<MyComponent> {

        public NoDefaultConstructorExporter(String tag) {
            super(tag);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent, MyComponent component) {

        }
    }
}
