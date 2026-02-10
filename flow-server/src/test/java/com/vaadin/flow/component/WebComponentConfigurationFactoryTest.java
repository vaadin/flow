/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.WebComponentExporterFactory.DefaultWebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;

class WebComponentConfigurationFactoryTest {

    private WebComponentExporter.WebComponentConfigurationFactory factory = new WebComponentExporter.WebComponentConfigurationFactory();

    @Test
    public void create_constructsValidConfiguration() {
        WebComponentConfiguration<? extends Component> config1 = factory.create(
                new DefaultWebComponentExporterFactory<WebComponentExporterTest.MyComponent>(
                        MyComponentExporter.class).create());

        WebComponentConfiguration<? extends Component> config2 = factory
                .create(new MyComponentExporter());

        Assertions.assertNotNull(config1,
                "create() from class should have been successful");
        Assertions.assertNotNull(config2,
                "create() from instance should have been " + "successful");
    }

    @Test
    public void create_instance_throwsOnNullArgument() {
        NullPointerException ex = Assertions.assertThrows(
                NullPointerException.class, () -> factory.create(null));
        Assertions.assertTrue(ex.getMessage().contains("'exporter'"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void create_configuration_hashCode() {
        WebComponentConfiguration<WebComponentExporterTest.MyComponent> myComponentConfig = factory
                .create(new DefaultWebComponentExporterFactory<WebComponentExporterTest.MyComponent>(
                        MyComponentExporter.class).create());
        WebComponentConfiguration<MyComponent> similarConfig1 = factory.create(
                new DefaultWebComponentExporterFactory<>(SimilarExporter1.class)
                        .create());
        WebComponentConfiguration<MyComponent> similarConfig2 = factory.create(
                new DefaultWebComponentExporterFactory<>(SimilarExporter2.class)
                        .create());
        WebComponentConfiguration<MyComponent> similarConfig3 = factory.create(
                new DefaultWebComponentExporterFactory<>(SimilarExporter3.class)
                        .create());

        Assertions.assertNotEquals(myComponentConfig.hashCode(),
                similarConfig1.hashCode(),
                "Configurations with different tags should have "
                        + "not have same hashCodes");

        Assertions.assertNotEquals(similarConfig1.hashCode(),
                similarConfig2.hashCode(),
                "Configurations with same tag, but different "
                        + "properties should not have same hashCodes");

        Assertions.assertEquals(similarConfig2.hashCode(),
                similarConfig3.hashCode(),
                "Configurations with same tag and same properties "
                        + "but different defaults should have the same hashCode");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void create_configuration_equals() {
        WebComponentConfiguration<WebComponentExporterTest.MyComponent> myComponentConfig = factory
                .create(new DefaultWebComponentExporterFactory<>(
                        MyComponentExporter.class).create());
        WebComponentConfiguration<MyComponent> similarConfig1 = factory.create(
                new DefaultWebComponentExporterFactory<>(SimilarExporter1.class)
                        .create());
        WebComponentConfiguration<MyComponent> similarConfig2 = factory.create(
                new DefaultWebComponentExporterFactory<>(SimilarExporter2.class)
                        .create());
        WebComponentConfiguration<MyComponent> similarConfig3 = factory.create(
                new DefaultWebComponentExporterFactory<>(SimilarExporter3.class)
                        .create());

        Assertions.assertNotEquals(myComponentConfig, similarConfig1,
                "Configurations with different tags should " + "not be equal");

        Assertions.assertNotEquals(similarConfig1, similarConfig2,
                "Configurations with same tag, but different "
                        + "properties should not be equal");

        // even though the classes are different, they define the same
        // embeddable web component
        Assertions.assertEquals(similarConfig2, similarConfig3,
                "Configurations with same tag and same properties "
                        + "but different defaults should be equal");
    }

    public static class MyComponent extends Component {

    }

    public static class MyComponentExporter
            extends WebComponentExporter<WebComponentExporterTest.MyComponent> {

        public MyComponentExporter() {
            super("my-component");
        }

        @Override
        public void configureInstance(
                WebComponent<WebComponentExporterTest.MyComponent> webComponent,
                WebComponentExporterTest.MyComponent component) {

        }
    }

    public static class NullTagExporter
            extends WebComponentExporter<MyComponent> {

        public NullTagExporter() {
            super(null);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    public static class NoDefaultConstructorExporter
            extends WebComponentExporter<MyComponent> {

        public NoDefaultConstructorExporter(String tag) {
            super(tag);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    public static class SimilarExporter1
            extends WebComponentExporter<MyComponent> {
        public SimilarExporter1() {
            super("tag");
            addProperty("string", "dog");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    public static class SimilarExporter2
            extends WebComponentExporter<MyComponent> {
        public SimilarExporter2() {
            super("tag");
            addProperty("int", 0);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    public static class SimilarExporter3
            extends WebComponentExporter<MyComponent> {
        public SimilarExporter3() {
            super("tag");
            addProperty("int", 1);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }
}
