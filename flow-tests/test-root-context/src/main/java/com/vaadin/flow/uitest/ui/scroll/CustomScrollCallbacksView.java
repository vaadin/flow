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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.CustomScrollCallbacksView", layout = ViewTestLayout.class)
public class CustomScrollCallbacksView extends AbstractDivView
        implements HasUrlParameter<String> {
    private final Div viewName = new Div();
    private final Div log = new Div();

    public CustomScrollCallbacksView() {
        viewName.setId("view");

        log.setId("log");
        log.getStyle().set("white-space", "pre");

        UI.getCurrent().getPage().executeJs(
                "window.Vaadin.Flow.setScrollPosition = function(xAndY) { $0.textContent += JSON.stringify(xAndY) + '\\n' }",
                log);
        UI.getCurrent().getPage().executeJs(
                "window.Vaadin.Flow.getScrollPosition = function() { return [42, -window.pageYOffset] }");

        RouterLink navigate = new RouterLink("Navigate",
                CustomScrollCallbacksView.class, "navigated");
        navigate.setId("navigate");

        Anchor back = new Anchor("javascript:history.go(-1)", "Back");
        back.setId("back");

        add(viewName, log, new Span("Scroll down to see navigation actions"),
                ScrollView.createSpacerDiv(2000), navigate, back);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        viewName.setText("Current view: " + parameter);
    }
}
