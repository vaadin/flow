/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.LongToOpenView", layout = ViewTestLayout.class)
public class LongToOpenView extends AbstractDivView {
    static final String BACK_BUTTON_ID = "backButton";
    static final String ANCHOR_LINK_ID = "anchorLinkId";

    public LongToOpenView() {
        Div div = new Div();
        div.setText("I am the long to open view");

        NativeButton back = createButton("Back", BACK_BUTTON_ID,
                event -> getPage().getHistory().back());

        add(div, back, ScrollView.createAnchorUrl(true, ANCHOR_LINK_ID,
                ScrollView.ANCHOR_URL, "Anchor url to other view"));
    }

    @Override
    protected void onShow() {
        try {
            // Delay is added to check that we don't jump to the beginning of
            // the page right after we click a link to this page.
            // We should update our scroll position only after the new page is
            // loaded.
            Thread.sleep(TimeUnit.SECONDS.toMillis(3L));
        } catch (InterruptedException e) {
            throw new IllegalStateException("Not supposed to be interrupted",
                    e);
        }
    }
}
