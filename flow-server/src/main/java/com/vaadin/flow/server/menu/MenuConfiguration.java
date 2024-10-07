/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow.server.menu;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.internal.menu.MenuRegistry;

/**
 * Menu configuration helper class to retrieve available menu entries for
 * application main menu.
 *
 * @since 24.5
 */
public final class MenuConfiguration {

    /**
     * Collect ordered list of menu entries for menu population. All client
     * views are collected and any accessible server views.
     *
     * @return ordered list of {@link MenuEntry} instances
     */
    public static List<MenuEntry> getMenuEntries() {
        return MenuRegistry.collectMenuItemsList().stream()
                .map(MenuConfiguration::createMenuEntry).toList();
    }

    /**
     * Collect ordered list of menu entries for menu population. All client
     * views are collected and any accessible server views.
     *
     * @param locale
     *            locale to use for ordering. null for default locale.
     *
     * @return ordered list of {@link MenuEntry} instances
     */
    public static List<MenuEntry> getMenuEntries(Locale locale) {
        return MenuRegistry.collectMenuItemsList(locale).stream()
                .map(MenuConfiguration::createMenuEntry).toList();
    }

    private static MenuEntry createMenuEntry(AvailableViewInfo viewInfo) {
        if (viewInfo.menu() == null) {
            return new MenuEntry(viewInfo.route(), viewInfo.title(), null, null,
                    null);
        }
        return new MenuEntry(viewInfo.route(),
                (viewInfo.menu().title() != null
                        && !viewInfo.menu().title().isBlank()
                                ? viewInfo.menu().title()
                                : viewInfo.title()),
                viewInfo.menu().order(), viewInfo.menu().icon(),
                viewInfo.menu().menuClass());
    }
}
