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

package com.vaadin.flow.server.menu;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.router.internal.RouteUtil;

/**
 * Menu configuration helper class to retrieve available menu entries for
 * application main menu.
 *
 * @since 24.5
 */
public final class MenuConfiguration {

    private static final String STATISTICS_DYNAMIC_MENU_ENTRIES = "flow/dynamic-menu-entries";

    /**
     * Collect ordered list of menu entries for menu population. All client
     * views are collected and any accessible server views.
     *
     * @return ordered list of {@link MenuEntry} instances
     */
    public static List<MenuEntry> getMenuEntries() {
        UsageStatistics.markAsUsed(STATISTICS_DYNAMIC_MENU_ENTRIES, null);
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
        UsageStatistics.markAsUsed(STATISTICS_DYNAMIC_MENU_ENTRIES, null);
        return MenuRegistry.collectMenuItemsList(locale).stream()
                .map(MenuConfiguration::createMenuEntry).toList();
    }

    /**
     * Retrieves the page header of the currently shown view. Can be used in
     * Flow main layouts to render a page header.
     * <p>
     * Attempts to retrieve header from the following sources:
     * <ul>
     * <li>from {@code ViewConfig.title} of the client-side views;</li>
     * <li>from {@link HasDynamicTitle#getPageTitle()} if present, then from
     * {@link PageTitle} value of the server-side route</li>
     * </ul>
     * <p>
     * For server-side routes it falls back to route's Java class name, if a
     * non-null {@code content} is given. For client-side views it falls back to
     * the React element's function name, if a page header couldn't be retrieved
     * from the {@code ViewConfig}.
     * <p>
     * Use {@link #getPageHeader()} method, if a content object is not
     * available.
     *
     * @param content
     *            as a {@link Component} class that represents a content in
     *            layout, can be {@code null}, if unavailable.
     * @return optional page header for layout
     */
    public static Optional<String> getPageHeader(Component content) {
        if (isServerSideContent(content)) {
            UI ui = UI.getCurrent();
            if (ui != null) {
                Optional<String> maybeTitle = RouteUtil.getDynamicTitle(ui);
                if (maybeTitle.isPresent()) {
                    return maybeTitle;
                }
            }

            return Optional.of(MenuRegistry.getTitle(content.getClass()));
        }
        return getPageHeaderFromMenuItems();
    }

    /**
     * Retrieves the page header of the currently shown view. Can be used in
     * Flow main layouts to render a page header.
     * <p>
     * Attempts to retrieve header from the following sources:
     * <ul>
     * <li>from {@code ViewConfig.title} of the client-side views;</li>
     * <li>from {@link HasDynamicTitle#getPageTitle()} if present, then from
     * {@link PageTitle} value of the server-side route</li>
     * </ul>
     * <p>
     * For server-side routes it falls back to route's Java class name. For
     * client-side views it falls back to the React element's function name, if
     * a page header couldn't be retrieved from the {@code ViewConfig}.
     * <p>
     * Note that the possible sources of page header are limited to only
     * available views in automatic menu configuration. If a route has a
     * mandatory route parameters or has a route template, then it won't be used
     * as a possible header source, even if it's shown.
     * <p>
     * Use {@link #getPageHeader(Component)} if content object is available,
     * e.g. in {@link com.vaadin.flow.router.RouterLayout} based layouts.
     *
     * @return optional page header for layout
     */
    public static Optional<String> getPageHeader() {
        return getPageHeader(null);
    }

    private static boolean isServerSideContent(Component content) {
        if (content == null) {
            return false;
        } else {
            Tag tag = content.getClass().getAnnotation(Tag.class);
            // client-side view if it is wrapped into ReactRouterOutlet
            return tag == null || !"react-router-outlet".equals(tag.value());
        }
    }

    private static Optional<String> getPageHeaderFromMenuItems() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            // Flow main layout + client views case:
            // layout may have dynamic title
            Optional<String> maybeTitle = RouteUtil.getDynamicTitle(ui);
            if (maybeTitle.isPresent()) {
                return maybeTitle;
            }

            String activeLocation = PathUtil.trimPath(
                    ui.getInternals().getActiveViewLocation().getPath());

            Map<String, AvailableViewInfo> menuItems = MenuRegistry
                    .getMenuItems(false);

            return menuItems.entrySet().stream()
                    .filter(menuEntry -> PathUtil.trimPath(menuEntry.getKey())
                            .equals(activeLocation))
                    .map(Map.Entry::getValue).map(AvailableViewInfo::title)
                    .filter(Objects::nonNull).findFirst();
        }
        return Optional.empty();
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
