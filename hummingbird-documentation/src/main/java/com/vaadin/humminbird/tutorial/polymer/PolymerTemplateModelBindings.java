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
package com.vaadin.humminbird.tutorial.polymer;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.hummingbird.template.model.TemplateModel;

@CodeFor("tutorial-template-bindings.asciidoc")
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
}
