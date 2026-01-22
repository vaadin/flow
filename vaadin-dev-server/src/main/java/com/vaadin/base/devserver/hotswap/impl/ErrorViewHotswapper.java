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
package com.vaadin.base.devserver.hotswap.impl;

import jakarta.annotation.Priority;

import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
import com.vaadin.base.devserver.hotswap.UIUpdateStrategy;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * Triggers UI refresh when hotswap occurs while an error view is displayed.
 * This ensures that fixing a broken class during development will refresh the
 * error page and attempt to re-navigate to the original location.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
@Priority(100)
public class ErrorViewHotswapper implements VaadinHotswapper {

    @Override
    public void onClassesChange(HotswapClassSessionEvent event) {
        // Only process redefined classes (not first-time loads)
        if (!event.isRedefined()) {
            return;
        }

        VaadinSession session = event.getVaadinSession();

        // Check each UI in the session
        for (UI ui : session.getUIs()) {
            if (ui.isClosing()) {
                continue;
            }

            // If showing error view, trigger refresh to re-attempt navigation
            if (ui.getInternals().isShowingErrorView()) {
                event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);
            }
        }
    }
}
