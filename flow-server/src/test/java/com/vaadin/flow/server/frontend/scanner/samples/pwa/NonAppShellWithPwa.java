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
package com.vaadin.flow.server.frontend.scanner.samples.pwa;

import com.vaadin.flow.server.PWA;

@PWA(name = "PWA Application", shortName = "PWA", description = "Testing PWA", display = "minimal-ui", backgroundColor = "#eee", themeColor = "#369", iconPath = "pwa.png", manifestPath = "appmanifest.json", offlinePath = "pwa.html", offlineResources = {
        "pwa.js", "pwa.css" })
public class NonAppShellWithPwa {
}
