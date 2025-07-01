/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.RelativeCoordinatesView", layout = ViewTestLayout.class)
public class RelativeCoordinatesView extends Div {

    public static final String CLICK_AREA_ID = "click-area";
    public static final String OUTPUT_ID = "output";

    public RelativeCoordinatesView() {
        Div clickArea = new Div();
        clickArea.setId(CLICK_AREA_ID);
        clickArea.setText("Click anywhere on this area to see relative coordinates");
        clickArea.getStyle()
            .set("background-color", "#f0f0f0")
            .set("border", "2px solid #ccc")
            .set("padding", "50px")
            .set("margin", "20px")
            .set("width", "400px")
            .set("height", "200px")
            .set("cursor", "pointer");

        Span output = new Span();
        output.setId(OUTPUT_ID);
        output.setText("Click on the area above to see coordinates");

        clickArea.addClickListener(this::handleClick);

        add(clickArea, output);
    }

    private void handleClick(ClickEvent<Div> event) {
        String coordinates = String.format(
            "Screen: (%d, %d), Client: (%d, %d), Relative: (%d, %d)",
            event.getScreenX(), event.getScreenY(),
            event.getClientX(), event.getClientY(),
            event.getRelativeX(), event.getRelativeY()
        );
        
        Span output = (Span) getChildren()
            .filter(component -> OUTPUT_ID.equals(component.getId().orElse("")))
            .findFirst()
            .orElse(null);
            
        if (output != null) {
            output.setText(coordinates);
        }
    }
}