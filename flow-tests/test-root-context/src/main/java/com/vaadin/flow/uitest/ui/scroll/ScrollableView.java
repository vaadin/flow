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
package com.vaadin.flow.uitest.ui.scroll;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.ScrollableView")
public class ScrollableView extends Div {

    public static final String TEST_VIEW_ID = "ScrollableView";

    public ScrollableView() {
        setId(TEST_VIEW_ID);

        Span button = new Span("Click to scroll");
        button.setId("button");
        button.addClickListener(e -> {
            getComponentAt(500).scrollIntoView();
        });
        add(button);

        for (int i = 0; i < 1000; i++) {
            Div div = new Div();
            div.setId("div-" + i);
            div.setText("div-" + i);
            add(div);
        }

        getComponentAt(500).scrollIntoView();

    }
}
