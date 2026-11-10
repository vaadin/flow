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
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.Size;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.signals.Signal;

public class ElementSizeComponent extends Div {

    static final int INITIAL_WIDTH = 120;
    static final int INITIAL_HEIGHT = 60;

    public ElementSizeComponent() {
        Div panel = new Div();
        panel.setId("panel");
        panel.getStyle().set("width", INITIAL_WIDTH + "px")
                .set("height", INITIAL_HEIGHT + "px")
                .set("box-sizing", "content-box");

        Div size = new Div();
        size.setId("size");

        Signal<Size> sizeSignal = panel.getElement().sizeSignal();
        Signal.effect(this, () -> {
            Size value = sizeSignal.get();
            size.setText(value.width() + "x" + value.height());
        });

        add(panel, size);
    }
}
