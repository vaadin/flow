/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.scroll;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.MultipleAnchorsView", layout = ViewTestLayout.class)
public class MultipleAnchorsView extends AbstractDivView {
    static final int NUMBER_OF_ANCHORS = 6;
    static final String ANCHOR_URL_ID_BASE = "anchorUrlId";
    static final String ANCHOR_DIV_ID_BASE = "anchorDivId";
    static final String ANCHOR_URL_BASE = MultipleAnchorsView.class
            .getCanonicalName() + '#' + ANCHOR_DIV_ID_BASE;

    public MultipleAnchorsView() {
        boolean isRouterLink = true;
        Div anchorDivContainer = new Div();

        for (int i = 0; i < NUMBER_OF_ANCHORS; i++) {
            String anchorDivId = ANCHOR_DIV_ID_BASE + i;
            Div anchorDiv = new Div();
            anchorDiv.setId(anchorDivId);
            anchorDiv.setText("I am an anchor div #" + i);

            Anchor anchorUrl = ScrollView.createAnchorUrl(isRouterLink,
                    ANCHOR_URL_ID_BASE + i,
                    MultipleAnchorsView.class.getCanonicalName() + '#'
                            + anchorDivId,
                    "Anchor url #" + i);
            isRouterLink = !isRouterLink;

            add(anchorUrl);
            anchorDivContainer.add(ScrollView.createSpacerDiv(200), anchorDiv);
        }

        add(ScrollView.createSpacerDiv(1000), anchorDivContainer,
                ScrollView.createSpacerDiv(1000));
    }
}
