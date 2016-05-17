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
package com.vaadin.humminbird.tutorial.webcomponent;

import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.event.ComponentEventListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;

@CodeFor("tutorial-webcomponent-events.asciidoc")
public class WebComponentEvents {
    @DomEvent("value-change")
    public class ValueChangeEvent extends ComponentEvent<PaperSlider> {
        public ValueChangeEvent(PaperSlider source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Tag("paper-slider")
    @HtmlImport("context://bower_components/paper-slider/paper-slider.html")
    public class PaperSlider extends Component {

        public PaperSlider() {
            getElement().synchronizeProperty("value", "value-change");
        }

        public void setValue(int value) {
            getElement().setProperty("value", value);
        }

        public int getValue() {
            return getElement().getProperty("value", 0);
        }

        public void setPin(boolean pin) {
            getElement().setProperty("pin", pin);
        }

        public boolean isPin() {
            return getElement().getProperty("pin", false);
        }

        public EventRegistrationHandle addValueChangeListener(
                ComponentEventListener<ValueChangeEvent> valueChangeListener) {
            // @formatter:off
            return super.addListener(ValueChangeEvent.class, valueChangeListener);
            // @formatter:on
        }
    }

}
