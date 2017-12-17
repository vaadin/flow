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

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("polymer-templates/tutorial-template-bindings.asciidoc")
public class PolymerTemplateModelBindings {

    @Tag("my-template")
    @HtmlImport("/com/example/PolymerBinding.html")
    public class PolymerBindingTemplate extends PolymerTemplate<BindingModel> {

        public PolymerBindingTemplate() {
            getModel().setHostProperty("Bound property");
        }
    }

    public interface BindingModel extends TemplateModel {
        void setHostProperty(String propertyValue);

        String getHostProperty();
    }

    @Tag("two-way-template")
    @HtmlImport("/com/example/PolymerTwoWayBinding.html")
    public class PolymerTwoWayBindingTemplate
            extends PolymerTemplate<TwoWayBindingModel> {

        public PolymerTwoWayBindingTemplate() {
            reset();
            //@formatter:off
            getElement().addPropertyChangeListener("name", event -> System.out
                    .println("Name is set to: " + getModel().getName()));
            getElement().addPropertyChangeListener("accepted",
                    event -> System.out.println("isAccepted is set to: "
                            + getModel().getAccepted()));
            getElement().addPropertyChangeListener("size", event -> System.out
                    .println("Size is set to: " + getModel().getSize()));
          //@formatter:on
        }

        @EventHandler
        private void reset() {
            getModel().setName("John");
            getModel().setAccepted(false);
            getModel().setSize("medium");
        }
    }

    public interface TwoWayBindingModel extends TemplateModel {
        void setName(String name);

        String getName();

        void setAccepted(Boolean accepted);

        Boolean getAccepted();

        void setSize(String size);

        String getSize();
    }
}
