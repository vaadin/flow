/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.component.internal;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

/**
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class ThemeHolder implements Serializable {
    AbstractTheme theme;

    private ThemeHolder(@Nullable AbstractTheme theme) {
        this.theme = theme;
    }

    public Optional<AbstractTheme> getTheme() {
        return Optional.ofNullable(theme);
    }

    /**
     * Finds current theme for given UI.
     *
     * @param ui
     *            the UI instance
     * @return holder of {@code AbstractTheme} value
     */
    static ThemeHolder of(UI ui) {
        VaadinContext context = ui.getSession().getService().getContext();
        return context.getAttribute(ThemeHolder.class, () -> {
            ThemeHolder holder = new ThemeHolder(findTheme(ui));
            context.setAttribute(ThemeHolder.class, holder);
            return holder;
        });
    }

    private static AbstractTheme findTheme(UI ui) {
        VaadinService service = ui.getSession().getService();
        VaadinContext context = service.getContext();
        final Class<? extends AppShellConfigurator> appShell = AppShellRegistry
                .getInstance(context).getShell();

        Class<? extends AbstractTheme> themeClass = null;
        if (appShell != null && appShell.getAnnotation(NoTheme.class) != null) {
            Theme themeAnnotation = appShell.getAnnotation(Theme.class);
            themeClass = themeAnnotation.themeClass();
            if (themeClass.equals(AbstractTheme.class)) {
                themeClass = getDefaultTheme(service);
            }
        } else {
            themeClass = getDefaultTheme(service);
        }
        if (themeClass != null) {
            return Instantiator.get(ui).getOrCreate(themeClass);
        }
        return null;
    }

    @Nullable
    private static Class<? extends AbstractTheme> getDefaultTheme(
            VaadinService service) {
        try {
            return (Class<? extends AbstractTheme>) service
                    .getClassLoader()
                    .loadClass(FrontendDependencies.LUMO);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
