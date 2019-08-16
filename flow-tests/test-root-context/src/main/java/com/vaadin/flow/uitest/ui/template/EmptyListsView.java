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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.ClientUpdateMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.EmptyListsView")
public class EmptyListsView extends AbstractDivView {

    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/EmptyLists.html")
    @JsModule("EmptyLists.js")
    @com.vaadin.flow.component.Tag("empty-list")
    public static class EmptyLists extends PolymerTemplate<EmptyListsModel> {
        public EmptyLists() {
            Item item = new Item();
            item.setLabel("foo");
            getModel().setItems(Collections.singletonList(item));
        }

        @Override
        protected EmptyListsModel getModel() {
            return super.getModel();
        }
    }

    public static class Tag {
        private String name;

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public static class Item {

        private String label;

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public List<Tag> getTags() {
            return new ArrayList<>();
        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public interface EmptyListsModel extends TemplateModel {
        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        List<Item> getItems();

        void setItems(List<Item> items);
    }

    public EmptyListsView() {
        EmptyLists template = new EmptyLists();
        template.setId("template");
        add(template);

        add(createButton("Set an empty list of items", "set-empty",
                event -> template.getModel().setItems(new ArrayList<>())));
    }
}
