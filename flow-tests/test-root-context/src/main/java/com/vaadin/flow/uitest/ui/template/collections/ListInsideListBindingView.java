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
import java.util.Objects;

import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.EventHandler;
import com.vaadin.ui.polymertemplate.ModelItem;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.ui.polymertemplate.RepeatIndex;

@Route(value = "com.vaadin.flow.uitest.ui.template.collections.ListInsideListBindingView", layout = ViewTestLayout.class)
@Tag("list-inside-list-binding")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/collections/ListInsideListBinding.html")
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
