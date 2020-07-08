/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("com.vaadin.flow.uitest.ui.BrokenRouterLinkView")
public class BrokenRouterLinkView extends AbstractDivView {

    public final static String LINK_ID = "broken-link";

    public BrokenRouterLinkView() {
        final RouterLink routerLink = new RouterLink("Broken",
                BrokenRouterLinkView.class);
        Div spacer = new Div();
        spacer.setHeight("5000px");
        add(spacer);

        routerLink.getElement().setAttribute("href", "somewhere_non_existent");
        routerLink.setId(LINK_ID);
        add(routerLink);
    }
}
