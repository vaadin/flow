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

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.flow.router.Route;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route("com.vaadin.flow.uitest.ui.template.EmptyListsView")
public class EmptyListsView extends AbstractDivView {

    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/EmptyLists.html")
    @com.vaadin.ui.Tag("empty-list")
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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

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

    public EmptyListsView() {
        EmptyLists template = new EmptyLists();
        template.setId("template");
        add(template);

        NativeButton button = new NativeButton("Set an empty list of items",
                event -> template.getModel().setItems(new ArrayList<>()));
        button.setId("set-empty");
        add(button);
    }
}
