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
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.RouterLinkTargetView", layout = ViewTestLayout.class)
public class RouterLinkTargetView extends Div {

    @Route(value = "com.vaadin.flow.uitest.ui.RouterLinkTargetView.TargetView", layout = ViewTestLayout.class)
    public static class TargetView extends Div {
        public TargetView() {
            NativeLabel label = new NativeLabel("Target view");
            label.setId("target");
            add(label);
        }
    }

    public RouterLinkTargetView() {
        RouterLink newTabLink = new RouterLink("Open in new tab",
                TargetView.class);
        newTabLink.setId("new-tab-link");
        newTabLink.setOpenInNewBrowserTab(true);

        RouterLink selfLink = new RouterLink("Open in this tab",
                TargetView.class);
        selfLink.setId("self-link");
        selfLink.setTarget("_self");

        add(newTabLink, new Paragraph(), selfLink);
    }
}
