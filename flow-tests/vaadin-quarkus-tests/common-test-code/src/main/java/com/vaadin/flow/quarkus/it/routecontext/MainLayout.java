/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.routecontext;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends Div implements RouterLayout {

    public static final String UIID = "UIID";

    public static final String PRESERVE = "preserve";
    public static final String INVALID = "invalid";
    public static final String PARENT_NO_OWNER = "parent-no-owner";
    public static final String CHILD_NO_OWNER = "child-no-owner";
    public static final String PROXIED = "proxied-route-scope";

    private Span uiIdLabel;

    public MainLayout() {
        add(new RouterLink(PRESERVE, PreserveOnRefreshView.class),
                new RouterLink(INVALID, InvalidView.class),
                new RouterLink(PARENT_NO_OWNER, ParentNoOwnerView.class),
                new RouterLink(CHILD_NO_OWNER, ChildNoOwnerView.class),
                new RouterLink(PROXIED, ProxiedView.class));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (uiIdLabel != null) {
            remove(uiIdLabel);
        }
        uiIdLabel = new Span(attachEvent.getUI().getUIId() + "");
        uiIdLabel.setId(UIID);
        add(uiIdLabel);
    }
}
