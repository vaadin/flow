/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.MultipleAnchorsView", layout = ViewTestLayout.class)
public class MultipleAnchorsView extends AbstractDivView {
    static final int NUMBER_OF_ANCHORS = 6;
    static final String ANCHOR_URL_ID_BASE = "anchorUrlId";
    static final String ANCHOR_DIV_ID_BASE = "anchorDivId";
    static final String ANCHOR_URL_BASE = MultipleAnchorsView.class.getCanonicalName() + '#' + ANCHOR_DIV_ID_BASE;

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

        add(ScrollView.createSpacerDiv(1000), anchorDivContainer, ScrollView.createSpacerDiv(1000));
    }
}
