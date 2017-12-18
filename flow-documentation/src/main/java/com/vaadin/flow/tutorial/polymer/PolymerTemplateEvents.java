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
package com.vaadin.flow.tutorial.polymer;

import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.ModelItem;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("polymer-templates/tutorial-template-event-handlers.asciidoc")
public class PolymerTemplateEvents {

    // @formatter:off
    @Tag("event-handler")
    @HtmlImport("/com/example/EventHandler.html")
    public class EventHandlerPolymerTemplate extends PolymerTemplate<TemplateModel> {

        @EventHandler
        private void handleClick() {
            System.out.println("Received a handle click event");
        }
    }


    @Tag("event-handler")
    @HtmlImport("/com/example/EventHandler.html")
    public class EventDataHandlerPolymerTemplate extends PolymerTemplate<TemplateModel> {

        @EventHandler
        private void handleClick(@EventData("event.altKey") boolean altPressed,
                @EventData("event.srcElement.tagName") String tag,
                @EventData("event.offsetX") int offsetX,
                @EventData("event.offsetY") int offsetY) {
            System.out.println("Event alt pressed: " + altPressed);
            System.out.println("Event tag: " + tag.toLowerCase(Locale.ENGLISH));
            System.out.println("Click position on element: [" + offsetX + ", "+ offsetY +"]");

        }
    }
  //@formatter:on

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

    public interface MessagesModel extends TemplateModel {
        void setMessages(List<Message> messages);
    }

    @Tag("model-item-handler")
    @HtmlImport("/com/example/ModelItemHandler.html")
    public class ModelItemHandlerPolymerTemplate
            extends PolymerTemplate<MessagesModel> {

        @EventHandler
        private void handleClick(@ModelItem Message message) {
            System.out.println("Received a message: " + message.getText());
        }
    }

    public static class UserInfo {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public interface Model extends TemplateModel {
        void setUserInfo(UserInfo userInfo);
    }

    @EventHandler
    private void onClick(
            @ModelItem("event.detail.userInfo") UserInfo userInfo) {
        System.err.println("contact : name = " + userInfo.getName());
    }
}
