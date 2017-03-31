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
import java.util.Comparator;
import java.util.List;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.hummingbird.uitest.ui.template.ListBindingTemplate.ListBindingModel;

@Tag("list-binding")
@HtmlImport("/com/vaadin/hummingbird/uitest/ui/template/ListBinding.html")
public class ListBindingTemplate extends PolymerTemplate<ListBindingModel> {

    public static class Message {
        private String text;

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
    }

    ListBindingTemplate() {
        getModel().setMessages(Collections.singletonList(new Message("foo")));
    }

    @EventHandler
    private void update() {
        getModel().setMessages(Arrays.asList(new Message("a"), new Message("b"),
                new Message("c")));
    }

    @EventHandler
    private void addElement() {
        getModel().getMessages().add(new Message("d1"));
    }

    @EventHandler
    private void addElementByIndex() {
        List<Message> currentMessages = getModel().getMessages();
        final int insertIndex = currentMessages.isEmpty() ? 0
                : currentMessages.size() - 1;
        currentMessages.add(insertIndex, new Message("d2"));
    }

    @EventHandler
    private void addNumerousElementsByIndex() {
        List<Message> newMessages = Arrays.asList(new Message("e1"),
                new Message("f1"));
        getModel().getMessages().addAll(0, newMessages);
    }

    @EventHandler
    private void addNumerousElementsAtTheEnd() {
        List<Message> newMessages = Arrays.asList(new Message("e2"),
                new Message("f2"));
        getModel().getMessages().addAll(newMessages);
    }

    @EventHandler
    private void clearList() {
        getModel().getMessages().clear();
    }

    @EventHandler
    private void removeElementByIndex() {
        List<Message> currentMessages = getModel().getMessages();
        if (!currentMessages.isEmpty()) {
            currentMessages.remove(currentMessages.size() - 1);
        }
    }

    @EventHandler
    private void removeElementViaIterator() {
        if (!getModel().getMessages().isEmpty()) {
            getModel().getMessages().iterator().remove();
        }
    }

    @EventHandler
    private void swapFirstAndLast() {
        List<Message> messages = getModel().getMessages();
        if (messages.size() > 1) {
            Message first = messages.get(0);
            messages.set(0, messages.get(messages.size() - 1));
            messages.set(messages.size() - 1, first);
        }
    }

    @EventHandler
    private void sortAlphabetically() {
        getModel().getMessages().sort(Comparator.comparing(Message::getText));
    }
}
