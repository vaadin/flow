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
public class AnchorView extends AbstractDivView {
    static final String ANCHOR_DIV_ID = "anchorDivId";
    static final String ANCHOR_URL = AnchorView.class.getCanonicalName() + '#' + ANCHOR_DIV_ID;
    static final String SIMPLE_ANCHOR_URL_ID = "simpleAnchorUrlId";
    static final String ROUTER_ANCHOR_URL_ID = "routerAnchorUrlId";

    public AnchorView() {
        Div spacer1 = new Div();
        spacer1.setText(
                "This is an intentionally long div (see urls at the bottom)");
        spacer1.setHeight("300px");

        Div anchorDiv = new Div();
        anchorDiv.setText("I'm the anchor div");
        anchorDiv.setId(ANCHOR_DIV_ID);

        Div spacer2 = new Div();
        spacer2.setText(
                "This is an intentionally long div (see urls at the bottom)");
        spacer2.setHeight("2000px");

        add(spacer1, anchorDiv, spacer2,
                createAnchorUrl(false, SIMPLE_ANCHOR_URL_ID),
                createAnchorUrl(true, ROUTER_ANCHOR_URL_ID));
    }

    private static Anchor createAnchorUrl(boolean isRouterLink, String id) {
        String text = "Go to anchor div";
        if (isRouterLink) {
            text += " (router link)";
        }
        Anchor result = new Anchor(ANCHOR_URL, text);
        result.getElement().setAttribute("router-link", true);
        result.getStyle().set("display", "block");
        result.setId(id);
        return result;
    }
}
