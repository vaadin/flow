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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.WebComponents;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

@WebComponents(1)
public class PolymerDefaultPropertyValueUI extends UI {

    public interface MyModel extends TemplateModel {
        void setText(String text);

        void setName(String name);
    }

    @Tag("default-property")
    @HtmlImport("/com/vaadin/flow/uitest/ui/template/PolymerDefaultPropertyValue.html")
    public static class MyTemplate extends PolymerTemplate<MyModel> {

        public MyTemplate() {
            getModel().setText("foo");
        }

    }

    @Override
    protected void init(VaadinRequest request) {
        MyTemplate template = new MyTemplate();
        template.setId("template");
        add(template);
    }
}
