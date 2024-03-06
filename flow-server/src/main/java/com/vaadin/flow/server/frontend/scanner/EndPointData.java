/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.router.Route;

/**
 * A simple container with the information related to an application end-point,
 * i.e. those classes annotated with the {@link Route} annotation.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public final class EndPointData implements Serializable {
    final String name;
    String route = "";
    String layout;
    ThemeData theme = new ThemeData();
    final LinkedHashSet<String> modules = new LinkedHashSet<>();
    final LinkedHashSet<String> themeModules = new LinkedHashSet<>();
    final LinkedHashSet<String> scripts = new LinkedHashSet<>();
    final transient List<CssData> css = new ArrayList<>();

    private final HashSet<String> classes = new HashSet<>();

    EndPointData(Class<?> clazz) {
        this.name = clazz.getName();
    }

    // For debugging
    @Override
    public String toString() {
        return String.format(
                "%n view: %s%n route: %s%n%s%n layout: %s%n modules: %s%n scripts: %s%n css: %s%n",
                name, route, theme, layout, col2Str(modules), col2Str(scripts),
                col2Str(css));
    }

    LinkedHashSet<String> getModules() {
        return modules;
    }

    LinkedHashSet<String> getThemeModules() {
        return themeModules;
    }

    LinkedHashSet<String> getScripts() {
        return scripts;
    }

    Set<String> getClasses() {
        return classes;
    }

    ThemeData getTheme() {
        return theme;
    }

    String getRoute() {
        return route;
    }

    String getLayout() {
        return layout;
    }

    String getName() {
        return name;
    }

    LinkedHashSet<CssData> getCss() {
        return new LinkedHashSet<>(css);
    }

    private String col2Str(Collection<?> s) {
        return String.join("\n          ", String.valueOf(s));
    }
}
