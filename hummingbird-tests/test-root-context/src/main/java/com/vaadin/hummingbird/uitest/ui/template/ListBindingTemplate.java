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
package com.vaadin.hummingbird.uitest.ui.template;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.ModelItem;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.html.Label;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.hummingbird.uitest.ui.template.ListBindingTemplate.ListBindingModel;

@Tag("list-binding")
@HtmlImport("/com/vaadin/hummingbird/uitest/ui/template/ListBinding.html")
public class ListBindingTemplate extends PolymerTemplate<ListBindingModel> {

    public static class Message {
        private String text;

        public Message(){}

        public Message(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public interface ListBindingModel extends TemplateModel {
        public void setMessages(List<Message> messages);
    }

    public ListBindingTemplate() {
        getModel().setMessages(Collections.singletonList(new Message("foo")));
    }

    @EventHandler
    private void update() {
        getModel().setMessages(Arrays.asList(new Message("a"), new Message("b"),
                new Message("c")));
    }

    @EventHandler
    private void selectItem(@ModelItem Message message) {
        Label label = new Label("Clicked message: " + message.getText());
        label.setId("selection");
        getElement().getParent().appendChild(
                label.getElement());
    }

}
