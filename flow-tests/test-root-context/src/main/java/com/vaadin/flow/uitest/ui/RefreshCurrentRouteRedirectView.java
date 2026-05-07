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

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.RefreshCurrentRouteRedirectView", layout = RefreshCurrentRouteLayout.class)
public class RefreshCurrentRouteRedirectView extends Div
        implements BeforeEnterObserver {

    enum RedirectMode {
        FORWARD, REROUTE
    }

    static class RedirectData {
        RedirectMode mode;
        int layoutCreationCount;

        RedirectData(RedirectMode mode) {
            this.mode = mode;
            this.layoutCreationCount = 0;
        }
    }

    static final String FORWARD_AND_REFRESH_LAYOUTS = "forward-refresh-layouts";
    static final String FORWARD_AND_REFRESH = "forward-refresh";
    static final String REROUTE_AND_REFRESH_LAYOUTS = "reroute-refresh-layouts";
    static final String REROUTE_AND_REFRESH = "reroute-refresh";

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RedirectData data = ComponentUtil.getData(event.getUI(),
                RedirectData.class);
        if (data == null) {
            return;
        }
        switch (data.mode) {
        case FORWARD ->
            event.forwardTo(RefreshCurrentRouteRedirectTargetView.class);
        case REROUTE ->
            event.rerouteTo(RefreshCurrentRouteRedirectTargetView.class);
        }
    }

    public RefreshCurrentRouteRedirectView() {
        addButton(FORWARD_AND_REFRESH_LAYOUTS, "Forward + refresh layouts",
                RedirectMode.FORWARD, true);
        addButton(FORWARD_AND_REFRESH, "Forward + refresh view only",
                RedirectMode.FORWARD, false);
        addButton(REROUTE_AND_REFRESH_LAYOUTS, "Reroute + refresh layouts",
                RedirectMode.REROUTE, true);
        addButton(REROUTE_AND_REFRESH, "Reroute + refresh view only",
                RedirectMode.REROUTE, false);
    }

    private void addButton(String id, String text, RedirectMode mode,
            boolean recreateLayouts) {
        NativeButton button = new NativeButton(text, e -> {
            UI ui = UI.getCurrent();
            ComponentUtil.setData(ui, RedirectData.class,
                    new RedirectData(mode));
            ui.refreshCurrentRoute(recreateLayouts);
        });
        button.setId(id);
        add(button);
    }
}
