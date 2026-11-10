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
package com.vaadin.flow.quarkus.it.menu;

import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;

/**
 * Lists the menu entries, which Flow collects from the routes annotated with
 * {@link Menu}. This view is the only one.
 */
@Route("menu")
@Menu(title = "Menu entries")
public class MenuView extends Span {

    public static final String MENU_ID = "menu";

    public MenuView() {
        setId(MENU_ID);
        setText(MenuConfiguration.getMenuEntries().stream()
                .map(MenuEntry::title).collect(Collectors.joining(",")));
    }
}
