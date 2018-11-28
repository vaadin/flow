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
package com.vaadin.flow.theme;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouteUtil;

/**
 *Utility class for getting default theme and navigation target theme.
 */
public final class ThemeUtil {

    public static final ThemeDefinition LUMO_CLASS_IF_AVAILABLE = loadLumoClassIfAvailable();

    /**
     * Loads the Lumo theme class from the classpath if it is available.
     *
     * @return the Lumo ThemeDefinition, or <code>null</code> if it is not
     * available in the classpath
     */
    private static final ThemeDefinition loadLumoClassIfAvailable() {
        try {
            Class<? extends AbstractTheme> theme = (Class<? extends AbstractTheme>) Class
                    .forName("com.vaadin.flow.theme.lumo.Lumo");
            return new ThemeDefinition(theme, "");
        } catch (ClassNotFoundException e) {
            // ignore, the Lumo class is not available in the classpath
            Logger logger = LoggerFactory.getLogger(RouteUtil.class.getName());
            logger.trace(
                    "Lumo theme is not present in the classpath. The application will not use any default theme.",
                    e);
        }
        return null;
    }

    private ThemeUtil() {
    }

    /**
     * Find annotated theme for navigationTarget on given path or lumo if
     * available.
     *
     * @param navigationTarget
     *         navigation target to find theme for
     * @param path
     *         path used for navigation
     * @return found theme or lumo if available
     */
    public static ThemeDefinition findThemeForNavigationTarget(
            Class<?> navigationTarget, String path) {

        if (navigationTarget == null) {
            return ThemeUtil.LUMO_CLASS_IF_AVAILABLE;
        }

        Class<? extends RouterLayout> topParentLayout = RouteUtil
                .getTopParentLayout(navigationTarget, path);

        Class<?> target =
                topParentLayout == null ? navigationTarget : topParentLayout;

        Optional<Theme> themeAnnotation = AnnotationReader
                .getAnnotationFor(target, Theme.class);

        if (themeAnnotation.isPresent()) {
            return new ThemeDefinition(themeAnnotation.get());
        }

        if (!AnnotationReader.getAnnotationFor(target, NoTheme.class)
                .isPresent()) {
            return ThemeUtil.LUMO_CLASS_IF_AVAILABLE;
        }

        return null;
    }
}
