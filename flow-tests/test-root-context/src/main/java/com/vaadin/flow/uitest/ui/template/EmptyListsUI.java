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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.WebComponents;
import com.vaadin.annotations.WebComponents.PolyfillVersion;
import com.vaadin.flow.html.Button;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

@WebComponents(PolyfillVersion.V1)
public class EmptyListsUI extends UI {

    @HtmlImport("/com/vaadin/flow/uitest/ui/template/EmptyLists.html")
    @com.vaadin.annotations.Tag("empty-list")
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
    }

    public static class Item {

        private String label;

        public List<Tag> getTags() {
            return new ArrayList<>();
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public interface EmptyListsModel extends TemplateModel {
        List<Item> getItems();

        void setItems(List<Item> items);
    }

    @Override
    protected void init(VaadinRequest request) {
        EmptyLists template = new EmptyLists();
        template.setId("template");
        add(template);

        Button button = new Button("Set an empty list of items",
                event -> template.getModel().setItems(new ArrayList<>()));
        button.setId("set-empty");
        add(button);
    }
}
