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
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;

/**
 * Pushes from a background thread after the page has been reloaded, which must
 * reach the browser through the connection of the reloaded page. Subclasses
 * select the transport and preserve the view on refresh.
 */
public abstract class AbstractPushAfterRefreshView
        extends AbstractTestViewWithLog implements AfterNavigationObserver {

    protected AbstractPushAfterRefreshView() {
        log("Init");
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (!event.isRefreshEvent()) {
            return;
        }
        log("Refresh");
        UI ui = event.getLocationChangeEvent().getUI();
        new Thread(() -> ui.accessSynchronously(() -> log("Push"))).start();
    }
}
