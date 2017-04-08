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

import java.util.Locale;

import com.vaadin.annotations.EventData;
import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.template.PolymerTemplate;

@CodeFor("tutorial-template-event-handlers.asciidoc")
public class PolymerTemplateEvents {
    @Tag("event-handler")
    @HtmlImport("/com/example/EventHandler.html")
    public class EventHandlerPolymerTemplate extends PolymerTemplate {

        @EventHandler
        private void handleClick() {
            System.out.println("Received a handle click event");
        }
    }

    @Tag("event-handler")
    @HtmlImport("/com/example/EventHandler.html")
    public class EventDataHandlerPolymerTemplate extends PolymerTemplate {

        @EventHandler
        private void sendData(@EventData("event.altKey") boolean altPressed,
                @EventData("event.srcElement.tagName") String tag,
                @EventData("event.offsetX") int offsetX,
                @EventData("event.offsetY") int offsetY) {
            System.out.println("Event alt pressed: " + altPressed);
            System.out.println("Event tag: " + tag.toLowerCase(Locale.ENGLISH));
            System.out.println("Click position on element: [" + offsetX + ", "+ offsetY +"]");
        }
    }
}
