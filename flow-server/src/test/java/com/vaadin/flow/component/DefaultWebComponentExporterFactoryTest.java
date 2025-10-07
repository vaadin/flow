/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.WebComponentExporterFactory.DefaultWebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class DefaultWebComponentExporterFactoryTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private class InnerClass extends WebComponentExporter<Component> {

        protected InnerClass() {
            super("a-b");
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {
        }

    }

    public static class NoSpecifiedTagClass
            extends WebComponentExporter<Component> {

        public NoSpecifiedTagClass() {
            super(null);
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {
        }

    }

    @Test(expected = NullPointerException.class)
    public void ctor_nullArg_throws() {
        new DefaultWebComponentExporterFactory<Component>(null);
    }

    @Test
    public void createInnerClass_throws() {
        exception.expect(RuntimeException.class);
        exception.expectCause(
                CoreMatchers.instanceOf(IllegalArgumentException.class));
        exception.expectMessage(
                CoreMatchers.containsString(InnerClass.class.getName()));
        exception.expectMessage(CoreMatchers.containsString("inner"));
        DefaultWebComponentExporterFactory<Component> factory = new DefaultWebComponentExporterFactory<>(
                InnerClass.class);

        factory.create();
    }

    @Test
    public void create_exporterHasNoTag_throws() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(CoreMatchers
                .containsString(NoSpecifiedTagClass.class.getCanonicalName()));
        exception.expectMessage(CoreMatchers
                .containsString("give null value to super(String)"));
        DefaultWebComponentExporterFactory<Component> factory = new DefaultWebComponentExporterFactory<>(
                NoSpecifiedTagClass.class);
        factory.create();
    }
}
