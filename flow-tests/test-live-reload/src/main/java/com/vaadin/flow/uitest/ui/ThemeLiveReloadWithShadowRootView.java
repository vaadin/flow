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
package com.vaadin.flow.uitest.ui;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ThemeLiveReloadWithShadowRootView", layout = ViewTestLayout.class)
@JsModule("./component-with-theme.ts")
public class ThemeLiveReloadWithShadowRootView extends AbstractLiveReloadView {

    public ThemeLiveReloadWithShadowRootView() {
        ApplicationConfiguration appConf = ApplicationConfiguration
                .get(VaadinService.getCurrent().getContext());
        Path stylesPath = Paths.get(
                appConf.getProjectFolder().getAbsolutePath(),
                FrontendUtils.DEFAULT_FRONTEND_DIR, "themes", "mytheme",
                "styles.css");
        Span span = new Span(stylesPath.toString());
        span.setId("styles.css");
        add(span);
        Div div1 = new Div();
        div1.setId("div1");
        div1.setText("This is div 1, it has a lightgreen background");
        add(div1);
        ComponentWithTheme c2 = new ComponentWithTheme();
        c2.setId("component-with-theme");
        add(c2);
    }

}
