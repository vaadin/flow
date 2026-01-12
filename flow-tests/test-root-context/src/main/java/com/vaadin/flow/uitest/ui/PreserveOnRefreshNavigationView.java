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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

@Route(value = PreserveOnRefreshNavigationView.VIEW_PATH)
@PreserveOnRefresh
public class PreserveOnRefreshNavigationView extends Div {

    static final String VIEW_PATH = "com.vaadin.flow.uitest.ui.PreserveOnRefreshNavigationView";

    public PreserveOnRefreshNavigationView() {
        add(createNavigationButton("one"));
        add(createNavigationButton("two"));
        add(createNavigationButton("three"));

        getElement().appendChild(createRouterLink("one"),
                createRouterLink("two"), createRouterLink("three"));
    }

    private NativeButton createNavigationButton(String param) {
        NativeButton button = new NativeButton("navigate to " + param,
                ev -> selfNavigate(param));
        button.setId("button-" + param);
        return button;
    }

    private void selfNavigate(String param) {
        UI.getCurrent().navigate(PreserveOnRefreshNavigationView.class,
                QueryParameters.of("param", param));
    }

    private Element createRouterLink(String param) {
        Element routerLink = ElementFactory.createRouterLink(
                VIEW_PATH + "?param=" + param, "link to " + param);
        routerLink.setAttribute("id", "link-" + param);
        return routerLink;
    }
}
