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
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;

@Meta(name = "foo", content = "bar")
@PWA(name = "My App", shortName = "app")
@Viewport(Viewport.DEVICE_DIMENSIONS)
@BodySize(height = "50vh", width = "50vw")
@Push(PushMode.AUTOMATIC)
@Theme("my-theme")
public class AppShell implements AppShellConfigurator {
    private final String url;

    public AppShell() {
        url = VaadinService.getCurrent().resolveResource("my-resource");
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addMetaTag("test-resource-url", url);
        settings.setPageTitle("app-shell-title");
    }
}
