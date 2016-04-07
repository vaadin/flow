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
package com.vaadin.hummingbird.uitest.component;

import java.util.function.Consumer;

import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.EventData;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;

public class AbstractHtmlComponent extends Component {

    public void setId(String id) {
        getElement().setAttribute("id", id);
    }

    public String getId() {
        return getElement().getAttribute("id");
    }

    public void addClass(String className) {
        getElement().getClassList().add(className);
    }

    protected void setText(String text) {
        getElement().setTextContent(text);
    }

    public EventRegistrationHandle addClickListener(
            Consumer<ClickEvent> listener) {
        return addListener(ClickEvent.class, listener);
    }

    @DomEvent("click")
    public static class ClickEvent extends ComponentEvent {

        private int clientX;
        private int clientY;

        public ClickEvent(AbstractHtmlComponent source, boolean fromClient,
                @EventData("event.clientX") int clientX,
                @EventData("event.clientY") int clientY) {
            super(source, fromClient);
            this.clientX = clientX;
            this.clientY = clientY;
        }

        @Override
        public AbstractHtmlComponent getSource() {
            return (AbstractHtmlComponent) super.getSource();
        }

        public int getClientX() {
            return clientX;
        }

        public int getClientY() {
            return clientY;
        }
    }

    @DomEvent("change")
    public static class ChangeEvent extends ComponentEvent {
        public ChangeEvent(AbstractHtmlComponent source, boolean fromClient) {
            super(source, fromClient);
        }
    }

}
