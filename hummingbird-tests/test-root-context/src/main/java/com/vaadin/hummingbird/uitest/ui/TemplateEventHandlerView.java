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

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.HtmlContainer;
import com.vaadin.hummingbird.html.Label;
import com.vaadin.hummingbird.router.View;

/**
 * @author Vaadin Ltd
 *
 */
public class TemplateEventHandlerView extends Div implements View {

    public static class EventReceiver extends InlineTemplate {

        private final HtmlContainer parent;

        EventReceiver(HtmlContainer parent) {
            super("<div id='event-receiver' (click)='$server.method()'>Click to send event to the server</div>");
            this.parent = parent;
        }

        @EventHandler
        protected void method() {
            Label label = new Label("Event is received");
            label.setId("event-handler");
            parent.add(label);
        }
    }

    public TemplateEventHandlerView() {
        add(new EventReceiver(this));
    }

}
