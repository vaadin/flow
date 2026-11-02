/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.example.application;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * Listens to a custom DOM event whose detail is read into a bean, which Jackson
 * does through reflection.
 */
@Route("dom-event")
public class DomEventView extends Div {

    public static final String FIRE_ID = "fire";
    public static final String RESULT_ID = "result";

    public static class Detail {
        private String name;
        private int count;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    @DomEvent("native-event")
    public static class NativeEvent extends ComponentEvent<DomEventView> {

        private final Detail detail;

        public NativeEvent(DomEventView source, boolean fromClient,
                @EventData("event.detail") Detail detail) {
            super(source, fromClient);
            this.detail = detail;
        }

        public Detail getDetail() {
            return detail;
        }
    }

    public DomEventView() {
        Span result = new Span();
        result.setId(RESULT_ID);
        addListener(NativeEvent.class,
                event -> result.setText(event.getDetail().getName() + " "
                        + event.getDetail().getCount()));
        NativeButton fire = new NativeButton("Fire the event",
                event -> getElement().executeJs(
                        "this.dispatchEvent(new CustomEvent('native-event', {detail: {name: $0, count: $1}}))",
                        "native", 3));
        fire.setId(FIRE_ID);
        add(fire, result);
    }
}
