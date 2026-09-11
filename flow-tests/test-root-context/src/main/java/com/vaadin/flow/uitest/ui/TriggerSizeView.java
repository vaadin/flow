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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.component.trigger.internal.SizeTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wires a {@link SizeTrigger} on a sized {@link Div} to two
 * {@link SetPropertyAction}s that mirror the current width and height into
 * separate display divs as their {@code textContent}. The browser dispatches a
 * resize on first observation, so the display divs populate without any user
 * interaction; resizing the panel via {@code style.width}/{@code style.height}
 * from the IT triggers a fresh dispatch that updates them again.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerSizeView", layout = ViewTestLayout.class)
public class TriggerSizeView extends AbstractDivView {

    static final int INITIAL_WIDTH = 120;
    static final int INITIAL_HEIGHT = 60;

    @Override
    protected void onShow() {
        Div panel = new Div();
        panel.setId("panel");
        // Explicit pixel size and content-box sizing so `contentRect` (which
        // SizeTrigger reads) reports the literal width/height — the IT
        // compares against these constants.
        panel.getStyle().set("width", INITIAL_WIDTH + "px")
                .set("height", INITIAL_HEIGHT + "px")
                .set("box-sizing", "content-box")
                .set("background", "lightblue");

        Div widthDiv = new Div();
        widthDiv.setId("width");

        Div heightDiv = new Div();
        heightDiv.setId("height");

        SizeTrigger resize = new SizeTrigger(panel);
        resize.triggers(
                new SetPropertyAction<>(widthDiv, "textContent",
                        SizeTrigger.EventData.width),
                new SetPropertyAction<>(heightDiv, "textContent",
                        SizeTrigger.EventData.height));

        add(panel, widthDiv, heightDiv);
    }
}
