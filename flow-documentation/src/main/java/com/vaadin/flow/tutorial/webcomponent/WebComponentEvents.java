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
package com.vaadin.flow.tutorial.webcomponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.HtmlImport;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("web-components/tutorial-webcomponent-events.asciidoc")
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
        }

        public void setValue(int value) {
            getElement().setProperty("value", value);
        }

        @Synchronize("value-change")
        public int getValue() {
            return getElement().getProperty("value", 0);
        }

        public void setPinVisible(boolean pinVisible) {
            getElement().setProperty("pin", pinVisible);
        }

        public boolean isPinVisible() {
            return getElement().getProperty("pin", false);
        }

        public Registration addValueChangeListener(
                ComponentEventListener<ValueChangeEvent> valueChangeListener) {
            // @formatter:off
            return super.addListener(ValueChangeEvent.class, valueChangeListener);
            // @formatter:on
        }
    }

}
