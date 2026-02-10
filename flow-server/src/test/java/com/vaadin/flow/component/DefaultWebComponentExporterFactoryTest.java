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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.WebComponentExporterFactory.DefaultWebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultWebComponentExporterFactoryTest {

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

    @Test
    public void ctor_nullArg_throws() {
        assertThrows(NullPointerException.class, () -> {
            new DefaultWebComponentExporterFactory<Component>(null);
        });
    }

    @Test
    public void createInnerClass_throws() {
        DefaultWebComponentExporterFactory<Component> factory = new DefaultWebComponentExporterFactory<>(
                InnerClass.class);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> factory.create());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getMessage().contains(InnerClass.class.getName()));
        assertTrue(ex.getMessage().contains("inner"));
    }

    @Test
    public void create_exporterHasNoTag_throws() {
        DefaultWebComponentExporterFactory<Component> factory = new DefaultWebComponentExporterFactory<>(
                NoSpecifiedTagClass.class);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> factory.create());
        assertTrue(ex.getMessage()
                .contains(NoSpecifiedTagClass.class.getCanonicalName()));
        assertTrue(
                ex.getMessage().contains("give null value to super(String)"));
    }
}
