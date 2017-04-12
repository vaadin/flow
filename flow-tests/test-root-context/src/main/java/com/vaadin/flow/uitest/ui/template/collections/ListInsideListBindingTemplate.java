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
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.ModelItem;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.html.Label;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;

@Tag("list-inside-list-binding")
@HtmlImport("/com/vaadin/flow/uitest/ui/template/collections/ListInsideListBinding.html")
public class ListInsideListBindingTemplate extends
        PolymerTemplate<ListInsideListBindingTemplate.ListInsideListBindingModel> {
    public static class Message {
        private String text;

        public Message() {
        }

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

    public interface ListInsideListBindingModel extends TemplateModel {
        void setNestedMessages(List<List<Message>> nested);

        List<List<Message>> getNestedMessages();
    }

    private final Label multiSelectionLabel;

    public ListInsideListBindingTemplate() {
        multiSelectionLabel = initLabel();

        getModel().setNestedMessages(Arrays.asList(
                Arrays.asList(new Message("a"), new Message("b"),
                        new Message("c")),
                Collections.singletonList(new Message("d"))));
    }

    private Label initLabel() {
        Label multiSelectionLabel;
        multiSelectionLabel = new Label();
        multiSelectionLabel.setId("multi-selection");
        getElement().getParent()
                .appendChild(multiSelectionLabel.getElement());
        return multiSelectionLabel;
    }

    @EventHandler
    private void selectedItems(@ModelItem List<Message> messages) {
        multiSelectionLabel.setText(buildMessageListString(messages));
    }

    private String buildMessageListString(List<Message> messages) {
        StringBuilder string = new StringBuilder();
        string.append("Clicked message List: ");
        string.append(messages.size()).append(" ");
        messages.forEach(item -> string.append(item.getText()));
        return string.toString();
    }

    @EventHandler
    private void test(@ModelItem Message clickedMessage) {
        System.out.println(clickedMessage.getText());
    }
}
