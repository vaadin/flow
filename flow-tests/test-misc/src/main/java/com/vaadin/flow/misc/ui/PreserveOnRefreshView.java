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

package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@PreserveOnRefresh
@Route("preserve")
public class PreserveOnRefreshView extends Div
        implements AfterNavigationObserver {

    private final Div uiId;

    public PreserveOnRefreshView() {

        uiId = new Div();
        uiId.setId("uiId");
        NativeButton reloadButton = new NativeButton("Reload page",
                ev -> UI.getCurrent().getPage().reload());
        reloadButton.setId("reload");
        add(new H1("This view is preserved on refresh"));
        add(new H3("Initial UI: " + UI.getCurrent().getUIId()));
        add(uiId, reloadButton);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        uiId.setText("UI: " + UI.getCurrent().getUIId());
    }
}
