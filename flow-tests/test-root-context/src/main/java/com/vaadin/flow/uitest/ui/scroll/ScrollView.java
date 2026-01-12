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

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.ScrollView", layout = ViewTestLayout.class)
public class ScrollView extends AbstractDivView {
    private static final String ROUTER_LINK_ATTRIBUTE_NAME = "router-link";

    static final String ANCHOR_DIV_ID = "anchorDivId";
    static final String ANCHOR_URL = ScrollView.class.getCanonicalName() + '#'
            + ANCHOR_DIV_ID;
    static final String SIMPLE_ANCHOR_URL_ID = "simpleAnchorUrlId";
    static final String ROUTER_ANCHOR_URL_ID = "routerAnchorUrlId";
    static final String TRANSITION_URL_ID = "transitionUrlId";

    public ScrollView() {
        Div anchorDiv = new Div();
        anchorDiv.setText("I'm the anchor div");
        anchorDiv.setId(ANCHOR_DIV_ID);

        add(createSpacerDiv(300), anchorDiv, createSpacerDiv(2000),
                createAnchorUrl(false, SIMPLE_ANCHOR_URL_ID, ANCHOR_URL,
                        "Go to anchor div (simple link)"),
                createAnchorUrl(true, ROUTER_ANCHOR_URL_ID, ANCHOR_URL,
                        "Go to anchor div (router-link)"),
                createAnchorUrl(true, TRANSITION_URL_ID,
                        LongToOpenView.class.getCanonicalName(),
                        "Go to different page (router-link)"));
    }

    static Anchor createAnchorUrl(boolean isRouterLink, String id, String url,
            String text) {
        Anchor result = new Anchor(url, text);
        result.getElement().setAttribute(ROUTER_LINK_ATTRIBUTE_NAME,
                isRouterLink);
        result.getStyle().set("display", "block");
        result.setId(id);
        return result;
    }

    static Div createSpacerDiv(int heightPx) {
        Div spacer = new Div();
        spacer.setHeight(heightPx + "px");
        return spacer;
    }
}
