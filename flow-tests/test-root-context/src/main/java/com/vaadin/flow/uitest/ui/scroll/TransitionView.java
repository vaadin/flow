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

package com.vaadin.flow.uitest.ui.scroll;

import com.vaadin.flow.html.Anchor;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.uitest.ui.AbstractDivView;

/**
 * @author Vaadin Ltd.
 */
public class TransitionView extends AbstractDivView {
    static final String URL_ID = "urlId";

    public TransitionView() {
        Div spacer = new Div();
        spacer.setText("This is an intentionally long div (see url at the bottom)");
        spacer.setHeight("2000px");

        Anchor url = new Anchor(
                "com.vaadin.flow.uitest.ui.scroll.LongToOpenView", "Go to LongToOpenView");
        url.getElement().setAttribute("router-link", true);
        url.getStyle().set("display", "block");
        url.setId(URL_ID);

        add(spacer, url);
    }
}
