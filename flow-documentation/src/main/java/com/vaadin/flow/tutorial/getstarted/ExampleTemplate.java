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
package com.vaadin.flow.tutorial.getstarted;

import com.vaadin.flow.tutorial.getstarted.ExampleTemplate.ExampleModel;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.polymertemplate.PolymerTemplate;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.flow.tutorial.annotations.CodeFor;

/**
 * Simple template example.
 */
@SuppressWarnings("serial")
@Tag("example-template")
@HtmlImport("ExampleTemplate.html")
@CodeFor("get-started.asciidoc")
public class ExampleTemplate extends PolymerTemplate<ExampleModel> {
    /**
     * Template model which defines the single "value" property.
     */
    public interface ExampleModel extends TemplateModel {

        void setValue(String value);
    }

    public ExampleTemplate() {
        // Set the initial value to the "value" property.
        getModel().setValue("Not clicked");
    }

    public void setValue(String value) {
        getModel().setValue(value);
    }
}
