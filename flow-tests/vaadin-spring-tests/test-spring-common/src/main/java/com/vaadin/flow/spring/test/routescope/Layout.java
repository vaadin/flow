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
package com.vaadin.flow.spring.test.routescope;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

public class Layout extends Div implements RouterLayout {

    public Layout() {
        add(createRouterLink("div", DivInLayout.class, "div-link"));
        add(createRouterLink("button", ButtonInLayout.class, "button-link"));
        add(createRouterLink("invalid", InvalidRouteScopeUsage.class,
                "invalid-route-link"));
    }

    private RouterLink createRouterLink(String text,
            Class<? extends Component> clazz, String id) {
        RouterLink link = new RouterLink(text, clazz);
        link.getStyle().set("display", "block");
        link.setId(id);
        return link;
    }
}
