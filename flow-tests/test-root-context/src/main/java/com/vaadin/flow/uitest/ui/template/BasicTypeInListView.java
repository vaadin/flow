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
package com.vaadin.flow.uitest.ui.template;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.BasicTypeInListView")
public class BasicTypeInListView extends AbstractDivView {

    @Tag("basic-type-list")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/BasicTypeList.html")
    @JsModule("BasicTypeList.js")
    public static class BasicTypeList extends PolymerTemplate<ItemsModel> {

        BasicTypeList() {
            getModel().setItems(Arrays.asList("foo", "bar"));
        }

        @Override
        protected ItemsModel getModel() {
            return super.getModel();
        }
    }

    public interface ItemsModel extends TemplateModel {
        void setItems(List<String> items);

        List<String> getItems();
    }

    public BasicTypeInListView() {
        BasicTypeList list = new BasicTypeList();
        list.setId("template");
        add(list);
        add(createButton("Add an item", "add",
                event -> list.getModel().getItems().add("newItem")));
        add(createButton("Remove the first item", "remove",
                event -> list.getModel().getItems().remove(0)));
    }
}
