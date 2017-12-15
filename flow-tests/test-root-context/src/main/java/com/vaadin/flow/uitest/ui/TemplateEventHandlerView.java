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
package com.vaadin.flow.uitest.ui;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.ui.common.ClientDelegate;
import com.vaadin.ui.common.HtmlContainer;

import elemental.json.JsonValue;

@Route(value = "com.vaadin.flow.uitest.ui.TemplateEventHandlerView", layout = ViewTestLayout.class)
public class TemplateEventHandlerView extends Div {

    public static class EventReceiver extends InlineTemplate<TemplateModel> {

        EventReceiver() {
            super("<div id='event-receiver' (click)='$server.method()'>Click to send event to the server</div>",
                    TemplateModel.class);
        }

        @ClientDelegate
        protected void method() {
            Label label = new Label("Event is received");
            label.setId("event-handler");
            getParentContainer().add(label);
        }

        private HtmlContainer getParentContainer() {
            return (HtmlContainer) getParent().get();
        }
    }

    public static class ArgReceiver extends InlineTemplate<TemplateModel> {

        ArgReceiver() {
            super("<div id='arg-receiver' (click)="
                    + "'$server.method($element.id, 3, 6.2, true, [2.1, 6.7],\"foo\",\"bar\" )'>"
                    + "Click to send event to the server</div>",
                    TemplateModel.class);
        }

        @ClientDelegate
        protected void method(String msg, int size, double value,
                boolean visible, Double[] array, String... vararg) {
            Label label = new Label("Event data is received");
            label.setId("event-arguments");
            getParentContainer().add(label);

            addLabel("event-msg-arg", msg);
            addLabel("event-int-arg", size);
            addLabel("event-double-arg", value);
            addLabel("event-boolean-arg", visible);
            addLabel("event-array-arg", Stream.of(array).map(Object::toString)
                    .collect(Collectors.joining(",")));
            addLabel("event-vararg-arg",
                    Stream.of(vararg).collect(Collectors.joining(",")));
        }

        private void addLabel(String id, Object value) {
            Label label = new Label(value.toString());
            label.getStyle().set("display", "block");
            label.setId(id);
            getParentContainer().add(label);
        }

        private HtmlContainer getParentContainer() {
            return (HtmlContainer) getParent().get();
        }
    }

    public static class JsonValueReceiver
            extends InlineTemplate<TemplateModel> {

        JsonValueReceiver() {
            super("<div id='json-receiver' (click)='$server.method({\"foo\":\"bar\"})'>"
                    + "Click to send json to the server</div>",
                    TemplateModel.class);
        }

        @ClientDelegate
        protected void method(JsonValue value) {
            Label label = new Label("Json object is received");
            label.setId("event-json");
            getParentContainer().add(label);

            Label jsonLabel = new Label(value.toJson());
            jsonLabel.getStyle().set("display", "block");
            jsonLabel.setId("json-arg");
            getParentContainer().add(jsonLabel);
        }

        private HtmlContainer getParentContainer() {
            return (HtmlContainer) getParent().get();
        }
    }

    public TemplateEventHandlerView() {
        add(new EventReceiver());
        add(new ArgReceiver());
        add(new JsonValueReceiver());
    }

}
