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
package com.vaadin.flow.uitest.servlet;

import java.lang.reflect.Field;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.startup.RouteRegistry;
import com.vaadin.flow.theme.ThemeDefinition;

public class ExcludeDefaultLumoUI extends UI {

    private static final Object LUMO_THEME = getDefaultLumoTheme();

    @Override
    public Optional<ThemeDefinition> getThemeFor(Class<?> navigationTarget,
            String path) {
        Optional<ThemeDefinition> theme = super.getThemeFor(navigationTarget,
                path);
        if (theme.isPresent() && theme.get().equals(LUMO_THEME)) {
            return Optional.empty();
        }
        return theme;
    }

    private static Object getDefaultLumoTheme() {
        try {
            Field field = RouteRegistry.class
                    .getDeclaredField("LUMO_CLASS_IF_AVAILABLE");
            field.setAccessible(true);
            return field.get(null);
        } catch (NoSuchFieldException | SecurityException
                | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
