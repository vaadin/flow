/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.nodefeature.ModelList;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.TemplateOverridesMap;
import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.Template;

/**
 * @author Vaadin Ltd
 *
 */
public class ForTemplateView extends Div implements View {

    public static class ForTemplate extends Template {

        public ForTemplate() {
            super(new ByteArrayInputStream(
                    "<ul><div></div><li *ngFor='let item of items' class='a'>{{item.text}}<input [value]='item.key'></li><div></div></ul>"
                            .getBytes(StandardCharsets.UTF_8)));
        }
    }

    public ForTemplateView() {
        ForTemplate template = new ForTemplate();

        StateNode modelListNode = new StateNode(ModelList.class);

        ModelList modelList = modelListNode.getFeature(ModelList.class);
        modelList.add(createModelItem("item1", "text1"));
        modelList.add(createModelItem("item2", "text2"));

        template.getElement().getNode().getFeature(ModelMap.class)
                .setValue("items", modelListNode);
        add(template);

        add(createButton("Append", "append",
                () -> modelList.add(createModelItem("appended", "append"))));
        add(createButton("Update first", "update-first",
                () -> updateModelItem(modelList.get(0), "Updated first",
                        "update first")));
        add(createButton("Update second", "udpate-second",
                () -> updateModelItem(modelList.get(1), "Updated second",
                        "update second")));
        add(createButton("Update last", "update-last",
                () -> updateModelItem(modelList.get(modelList.size() - 1),
                        "Updated last", "update last")));
        add(createButton("Delete first", "delete-first",
                () -> modelList.remove(0)));
    }

    private Button createButton(String text, String id, Runnable action) {
        Button button = new Button(text);
        button.addClickListener(e -> action.run());
        button.setId(id);
        return button;
    }

    private void updateModelItem(StateNode stateNode, String text, String key) {
        stateNode.getFeature(ModelMap.class).setValue("text", text);
        stateNode.getFeature(ModelMap.class).setValue("key", key);
    }

    private StateNode createModelItem(String text, String key) {
        StateNode modelItem1 = new StateNode(TemplateOverridesMap.class,
                ModelMap.class);
        updateModelItem(modelItem1, text, key);
        return modelItem1;

    }
}
