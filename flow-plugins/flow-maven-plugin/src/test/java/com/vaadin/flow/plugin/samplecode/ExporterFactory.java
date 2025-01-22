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
package com.vaadin.flow.plugin.samplecode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class ExporterFactory implements WebComponentExporterFactory<Component> {

    private class InnerExporter extends WebComponentExporter<Component> {

        private InnerExporter(String tag) {
            super(tag);
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {
        }

    }

    @Override
    public WebComponentExporter<Component> create() {
        return new InnerExporter("wc-foo-bar");
    }

}
