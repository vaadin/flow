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
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.ModelItem;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.ClientUpdateMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.template.collections.ListBindingView.ListBindingModel;

@Route(value = "com.vaadin.flow.uitest.ui.template.collections.ListBindingView", layout = ViewTestLayout.class)
@Tag("list-binding")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/collections/ListBinding.html")
@JsModule("ListBinding.js")
public class ListBindingView extends PolymerTemplate<ListBindingModel> {
    static final List<String> RESET_STATE = Arrays.asList("1", "2", "3");
    static final String INITIAL_STATE = "foo";

    public interface ListBindingModel extends TemplateModel {
        void setSelectedMessage(Message selectedMessage);

        void setMessages(List<Message> messages);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        List<Message> getMessages();
    }

    public ListBindingView() {
        setId("template");
        getModel().setMessages(
                Collections.singletonList(new Message(INITIAL_STATE)));
    }

    @EventHandler
    private void reset() {
        getModel().setMessages(RESET_STATE.stream().map(Message::new)
                .collect(Collectors.toList()));
    }

    @EventHandler
    private void selectItem(@ModelItem Message message) {
        getModel().setSelectedMessage(message);
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
