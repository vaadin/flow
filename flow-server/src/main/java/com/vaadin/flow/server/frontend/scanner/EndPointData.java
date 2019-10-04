/*
 * Copyright 2000-2018 Vaadin Ltd.
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
