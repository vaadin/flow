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
package com.vaadin.flow.spring.flowsecurity;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;

import static com.vaadin.flow.spring.flowsecurity.Configurator.ICONS_PATH;

@PWA(name = "Spring Security Helper Test Project", shortName = "SSH Test", iconPath = ICONS_PATH
        + "hey.png")
public class Configurator implements AppShellConfigurator {

    public static final String ICONS_PATH = "custom/icons/path/";

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon("icon", "/" + ICONS_PATH + "fav.ico", "32x32");
    }
}
