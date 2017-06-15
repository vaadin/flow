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
package com.vaadin.flow.demo;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.demo.views.DemoView;
import com.vaadin.flow.html.Anchor;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.View;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.Component;

/**
 * Main layout of the application. It contains the menu, header and the main
 * section of the page.
 */
@Tag("main-layout")
@HtmlImport("frontend://src/main-layout.html")
public class MainLayout extends Component implements HasChildView {

    private View selectedView;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (attachEvent.isInitialAttach()) {
            appendViewAnchor("paper-button", "Paper Button");
            appendViewAnchor("paper-input", "Paper Input");
        }
    }

    private void appendViewAnchor(String href, String text) {
        Anchor anchor = new Anchor(href, text);
        anchor.getElement().setProperty("slot", "selectors");
        anchor.getElement().setProperty("name", text);
        anchor.getElement().setAttribute("router-link", true);
        getElement().appendChild(anchor.getElement());
    }

    @Override
    public void setChildView(View childView) {
        if (selectedView == childView) {
            return;
        }
        if (selectedView != null) {
            selectedView.getElement().removeFromParent();
        }
        selectedView = childView;

        // uses the <slot> at the template
        getElement().appendChild(childView.getElement());
        if (childView instanceof DemoView) {
            getElement().setProperty("page",
                    ((DemoView) childView).getViewName());
        }
    }
}
