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
package com.vaadin.flow.i18n;

import java.util.ResourceBundle;

import com.vaadin.flow.hotswap.HotswapResourceEvent;
import com.vaadin.flow.hotswap.UIUpdateStrategy;
import com.vaadin.flow.hotswap.VaadinHotswapper;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Handles the automatic hotswapping of translation resources in a Vaadin-based
 * application. This class listens for changes in application resources and
 * takes appropriate actions to update related translations dynamically.
 * <p>
 * When a resource file matching the path pattern for translation properties
 * (e.g., `vaadin-i18n/*.properties`) is changed, this class performs two main
 * actions: 1. Clears the {@link ResourceBundle} cache to ensure the latest
 * translations are loaded. 2. Notifies the client about the translation updates
 * by triggering an appropriate update event and UI refreshes.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public class DefaultTranslationsHotswapper implements VaadinHotswapper {

    @Override
    public void onResourcesChange(HotswapResourceEvent event) {
        if (event.anyMatches(".*/vaadin-i18n/.*\\.properties")) {
            // Clear resource bundle cache so that translations (and other
            // resources) are reloaded
            ResourceBundle.clearCache();

            // Trigger any potential Hilla translation updates
            event.sendHmrEvent("translations-update",
                    JacksonUtils.createObjectNode());

            // Trigger any potential Flow translation updates
            event.triggerUpdate(UIUpdateStrategy.REFRESH);
        }
    }
}
