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
package com.vaadin.flow.tutorial.polymer;

import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.polymertemplate.EventHandler;
import com.vaadin.flow.polymertemplate.PolymerTemplate;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("polymer-templates/tutorial-template-subtemplate.asciidoc")
public class TemplateInTemplate {

    @Tag("parent-template")
    @HtmlImport("/com/example/ParentTemplate.html")
    public class ParentTemplate extends PolymerTemplate<Model> {
    }

    public interface Model extends TemplateModel {
        void setName(String name);

        String getName();
    }

    @Tag("child-template")
    @HtmlImport("/com/example/ChildTemplate.html")
    public class ChildTemplate extends PolymerTemplate<TemplateModel> {

        @EventHandler
        private void handleClick() {
            System.out.println("Click on Button in the child template");
        }
    }
}
