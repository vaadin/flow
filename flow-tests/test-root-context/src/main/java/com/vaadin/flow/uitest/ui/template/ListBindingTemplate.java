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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.ModelItem;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.html.Label;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.flow.uitest.ui.template.ListBindingTemplate.ListBindingModel;

@Tag("list-binding")
@HtmlImport("/com/vaadin/flow/uitest/ui/template/ListBinding.html")
public class ListBindingTemplate extends PolymerTemplate<ListBindingModel> {
    static final List<String> RESET_STATE = Arrays.asList("1", "2", "3");
    static final String INITIAL_STATE = "foo";

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

    public interface ListBindingModel extends TemplateModel {
        void setMessages(List<Message> messages);

        List<Message> getMessages();

        void setNestedMessages(List<List<Message>> nested);
    }

    private Label multiselectionLabel;

    public ListBindingTemplate() {
        getModel().setMessages(
                Collections.singletonList(new Message(INITIAL_STATE)));
        getModel().setNestedMessages(Arrays.asList(
                Arrays.asList(new Message("a"), new Message("b"),
                        new Message("c")),
                Collections.singletonList(new Message("d"))));
    }

    @EventHandler
    private void reset() {
        getModel().setMessages(RESET_STATE.stream().map(Message::new)
                .collect(Collectors.toList()));
    }

    @EventHandler
    private void selectItem(@ModelItem Message message) {
        Label label = new Label("Clicked message: " + message.getText());
        label.setId("selection");
        getElement().getParent().appendChild(label.getElement());
    }

    @EventHandler
    private void selectedItems(@ModelItem List<Message> messages) {
        if (multiselectionLabel == null) {
            multiselectionLabel = new Label(buildMessageListString(messages));
            multiselectionLabel.setId("multi-selection");
            getElement().getParent()
                    .appendChild(multiselectionLabel.getElement());
        } else {
            multiselectionLabel.setText(buildMessageListString(messages));
        }
    }

    private String buildMessageListString(List<Message> messages) {
        StringBuilder string = new StringBuilder();
        string.append("Clicked message List: ");
        string.append(messages.size()).append(" ");
        messages.forEach(item -> string.append(item.getText()));
        return string.toString();
    }

    @EventHandler
    private void addElement() {
        getModel().getMessages().add(new Message("4"));
    }

    @EventHandler
    private void addElementByIndex() {
        getModel().getMessages().add(0, new Message("4"));
    }

    @EventHandler
    private void addNumerousElements() {
        List<Message> newMessages = Arrays.asList(new Message("4"),
                new Message("5"));
        getModel().getMessages().addAll(newMessages);
    }

    @EventHandler
    private void addNumerousElementsByIndex() {
        List<Message> newMessages = Arrays.asList(new Message("4"),
                new Message("5"));
        getModel().getMessages().addAll(0, newMessages);
    }

    @EventHandler
    private void clearList() {
        getModel().getMessages().clear();
    }

    @EventHandler
    private void removeSecondElementByIndex() {
        List<Message> currentMessages = getModel().getMessages();
        if (currentMessages.size() > 2) {
            currentMessages.remove(1);
        }
    }

    @EventHandler
    private void removeFirstElementWithIterator() {
        if (!getModel().getMessages().isEmpty()) {
            Iterator<Message> iterator = getModel().getMessages().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    @EventHandler
    private void swapFirstAndSecond() {
        List<Message> messages = getModel().getMessages();
        if (messages.size() > 1) {
            Message first = messages.get(0);
            messages.set(0, messages.get(1));
            messages.set(1, first);
        }
    }

    @EventHandler
    private void sortDescending() {
        getModel().getMessages()
                .sort(Comparator.comparing(Message::getText).reversed());
    }

    @EventHandler
    private void setInitialStateToEachMessage() {
        getModel().getMessages()
                .forEach(message -> message.setText(INITIAL_STATE));
    }
}
