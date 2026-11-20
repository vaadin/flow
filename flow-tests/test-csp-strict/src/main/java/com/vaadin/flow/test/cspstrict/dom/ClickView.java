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
package com.vaadin.flow.test.cspstrict.dom;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * The most ordinary thing an application does: a click listener on a component,
 * whose event carries data read from the DOM event, a DOM event listener with
 * an event data path on a plain element, and a component event whose event data
 * and filter are expressions that the build puts into the bundle, also for a
 * component that the expression returns.
 */
@Route("click")
public class ClickView extends Div {

    public ClickView() {
        Span clicked = new Span();
        clicked.setId("clicked");
        NativeButton button = new NativeButton("Click me",
                event -> clicked.setText("clicked at x=" + event.getClientX()));
        button.setId("button");

        Span clientX = new Span();
        clientX.setId("client-x");
        Div area = new Div("Click the area");
        area.setId("area");
        area.getElement().addEventListener("click",
                event -> clientX.setText("x="
                        + event.getEventData().get("event.clientX").asInt()))
                .addEventDataPath("event.clientX");

        Span offset = new Span();
        offset.setId("offset");
        Span marker = new Span("Click here too");
        marker.setId("offset-marker");
        marker.addClassName("marker");
        Div offsetArea = new Div(marker);
        offsetArea.setId("offset-area");
        ComponentUtil.addListener(offsetArea, OffsetClickEvent.class,
                event -> offset.setText("offset=" + event.getOffset()
                        + " marker=" + event.getMarker().getId().orElse("")));

        add(button, clicked, area, clientX, offsetArea, offset);
    }

    /**
     * A click whose event data is computed by an expression rather than read
     * from a property path, and a component found by an expression, which the
     * client maps from the DOM node that the expression returns. The filter is
     * an expression too, and also decides whether the default is prevented.
     */
    @DomEvent(value = "click", filter = "event.button === 0", preventDefault = true)
    public static class OffsetClickEvent extends ComponentEvent<Div> {
        private final int offset;
        private final Span marker;

        public OffsetClickEvent(Div source, boolean fromClient,
                @EventData("Math.round(event.clientX - element.getBoundingClientRect().left)") int offset,
                @EventData("element.querySelector('.marker')") Span marker) {
            super(source, fromClient);
            this.offset = offset;
            this.marker = marker;
        }

        public int getOffset() {
            return offset;
        }

        public Span getMarker() {
            return marker;
        }
    }
}
