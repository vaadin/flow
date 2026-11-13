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
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.server.ContentSecurityPolicy;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Allows the Google Fonts stylesheet that the application theme imports, and
 * the font files that stylesheet loads, in the Content Security Policy that
 * Vaadin sends in the strict mode.
 */
public class ExternalFontsPolicy implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(response -> {
            ContentSecurityPolicy policy = response.getContentSecurityPolicy();
            if (policy != null) {
                policy.addSources("style-src", "https://fonts.googleapis.com")
                        .addSources("font-src", "https://fonts.gstatic.com");
            }
        });
    }
}
