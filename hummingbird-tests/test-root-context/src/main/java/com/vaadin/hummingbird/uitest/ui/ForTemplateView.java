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
package com.vaadin.hummingbird.uitest.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.annotations.ClientDelegate;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.HtmlContainer;
import com.vaadin.hummingbird.html.Label;
import com.vaadin.hummingbird.nodefeature.ModelList;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.TemplateOverridesMap;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.server.Command;

/**
 * @author Vaadin Ltd
 *
 */
public class ForTemplateView extends Div implements View {

    public static class Item {
        private String text;
        private String key;

        public Item(String text, String key) {
            this.text = text;
            this.key = key;
        }

        public String getText() {
            return text;
        }

        public String getKey() {
            return key;
        }
    }

    public interface ItemListModel extends TemplateModel {
        void setItems(List<Item> modelList);
    }

    public static class ForTemplate extends InlineTemplate<ItemListModel> {

        public ForTemplate() {
            super("<ul><div></div>"
                    + "<li *ngFor='let item of items' class='a' (click)='$server.callRpc()'>{{item.text}}"
                    + "<input [value]='item.key'></li><div></div></ul>",
                    ItemListModel.class);
        }

        @ClientDelegate
        private void callRpc() {
            Label label = new Label("Server Event Handler is called");
            label.setId("server-rpc");
            ((HtmlContainer) getParent().get()).add(label);
        }
    }

    public ForTemplateView() {
        ForTemplate template = new ForTemplate();

        List<Item> items = new ArrayList<>();
        items.add(new Item("item1", "text1"));
        items.add(new Item("item2", "text2"));
        template.getModel().setItems(items);

        add(template);

        // Still needs low-level ModelList for updates
        StateNode modelListNode = (StateNode) template.getElement().getNode()
                .getFeature(ModelMap.class).getValue("items");
        ModelList modelList = modelListNode.getFeature(ModelList.class);
        add(createButton("Append", "append",
                () -> modelList.add(createModelItem("appended", "append"))));
        add(createButton("Update first", "update-first",
                () -> updateModelItem(modelList.get(0), "Updated first",
                        "update first")));
        add(createButton("Update second", "update-second",
                () -> updateModelItem(modelList.get(1), "Updated second",
                        "update second")));
        add(createButton("Update last", "update-last",
                () -> updateModelItem(modelList.get(modelList.size() - 1),
                        "Updated last", "update last")));
        add(createButton("Delete first", "delete-first",
                () -> modelList.remove(0)));
    }

    private Button createButton(String text, String id, Command action) {
        Button button = new Button(text);
        button.addClickListener(e -> action.execute());
        button.setId(id);
        return button;
    }

    private void updateModelItem(StateNode stateNode, String text, String key) {
        ModelMap model = ModelMap.get(stateNode);
        model.setValue("text", text);
        model.setValue("key", key);
    }

    private StateNode createModelItem(String text, String key) {
        StateNode modelItem1 = new StateNode(TemplateOverridesMap.class,
                ModelMap.class);
        updateModelItem(modelItem1, text, key);
        return modelItem1;

    }
}
