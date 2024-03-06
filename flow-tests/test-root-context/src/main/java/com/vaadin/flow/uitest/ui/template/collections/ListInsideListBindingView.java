/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.ModelItem;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.RepeatIndex;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.ClientUpdateMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.collections.ListInsideListBindingView", layout = ViewTestLayout.class)
@Tag("list-inside-list-binding")
@JsModule("./ListInsideListBinding.js")
public class ListInsideListBindingView extends
        PolymerTemplate<ListInsideListBindingView.ListInsideListBindingModel> {
    static final String UPDATED_TEXT = "test";

    public static class Message {
        private String text;

        public Message() {
        }

        public Message(String text) {
            this.text = text;
        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public interface ListInsideListBindingModel extends TemplateModel {
        void setRemovedMessage(Message removedMessage);

        void setNestedMessages(List<List<Message>> nested);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        List<List<Message>> getNestedMessages();
    }

    public ListInsideListBindingView() {
        setId("template");
        setInitialState();
    }

    private void setInitialState() {
        getModel().setNestedMessages(Arrays.asList(
                Arrays.asList(new Message("a"), new Message("b"),
                        new Message("c")),
                Collections.singletonList(new Message("d"))));
    }

    @EventHandler
    private void removeItem(@ModelItem Message clickedMessage,
            @RepeatIndex int itemIndex) {
        getModel().getNestedMessages()
                .forEach(list -> removeMessageIfContainedInList(clickedMessage,
                        itemIndex, list));
    }

    private void removeMessageIfContainedInList(Message clickedMessage,
            int itemIndex, List<Message> list) {
        if (list.size() > itemIndex && Objects.equals(
                list.get(itemIndex).getText(), clickedMessage.getText())) {
            Message removedMessage = list.remove(itemIndex);
            getModel().setRemovedMessage(removedMessage);
        }
    }

    @EventHandler
    private void reset() {
        setInitialState();
    }

    @EventHandler
    private void updateAllElements() {
        getModel().getNestedMessages().forEach(
                list -> list.forEach(message -> message.setText(UPDATED_TEXT)));
    }
}
