/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.pwatest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.PWA;

@PWA(name = ParentLayout.PWA_NAME,
        shortName = ParentLayout.PWA_SHORT_NAME,
        themeColor = ParentLayout.THEME_COLOR,
        backgroundColor = ParentLayout.BG_COLOR,
        offlinePath = "offline.html")
public class ParentLayout extends Div
        implements RouterLayout, AppShellConfigurator {
    static final String THEME_COLOR = "#1f1f1f";
    static final String BG_COLOR = "#ffffff";
    static final String PWA_NAME = "PWA test name";
    static final String PWA_SHORT_NAME = "PWA";

    public ParentLayout() {
    }
}
