/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.theme;

import java.util.List;
import java.util.Optional;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouteUtil;

/**
 * Utility class for getting default theme and navigation target theme.
 *
 * @since 1.3
 */
public final class ThemeUtil {

    private static final boolean IS_OSGI_ENV = isInOSGi();

    private ThemeUtil() {
    }

    /**
     * Gets the {@code Lumo} theme definition (default theme) if it's available
     * in the classpath.
     *
     * @return an optional {@code Lumo} theme definition or an empty optional if
     *         it's not in the classpath
     */
    public static Optional<ThemeDefinition> getLumoThemeDefinition() {
        if (IS_OSGI_ENV) {
            Bundle bundle = FrameworkUtil.getBundle(ThemeDefinition.class);
            if (bundle == null) {
                return Optional
                        .ofNullable(LazyLoadLumoTheme.LUMO_CLASS_IF_AVAILABLE);
            }
            BundleContext context = bundle.getBundleContext();

            ServiceReference<ThemeDefinition> reference = context
                    .getServiceReference(ThemeDefinition.class);
            return Optional.ofNullable(reference).map(context::getService);
        }
        return Optional.ofNullable(LazyLoadLumoTheme.LUMO_CLASS_IF_AVAILABLE);
    }

    /**
     * Find annotated theme for navigationTarget on given path or lumo if
     * available.
     *
     * @param ui
     *            the UI where {@code navigationTarget} is expected being
     *            registered
     * @param navigationTarget
     *            navigation target to find theme for
     * @param path
     *            path used for navigation
     * @return found theme or lumo if available
     */
    public static ThemeDefinition findThemeForNavigationTarget(UI ui,
            Class<?> navigationTarget, String path) {
        if (navigationTarget == null) {
            return getLumoThemeDefinition().orElse(null);
        }

        Class<? extends RouterLayout> topParentLayout = null;
        if (Component.class.isAssignableFrom(navigationTarget)) {
            List<Class<? extends RouterLayout>> routeLayouts = ui.getInternals()
                    .getRouter().getRegistry().getRouteLayouts(path,
                            (Class<? extends Component>) navigationTarget);
            topParentLayout = routeLayouts.isEmpty() ? null
                    : routeLayouts.get(routeLayouts.size() - 1);
        }

        Class<?> target = topParentLayout == null ? navigationTarget
                : topParentLayout;

        Optional<Theme> themeAnnotation = AnnotationReader
                .getAnnotationFor(target, Theme.class);

        if (themeAnnotation.isPresent()) {
            final ThemeDefinition themeDefinition = new ThemeDefinition(
                    themeAnnotation.get());
            return themeDefinition;
        }

        if (!AnnotationReader.getAnnotationFor(target, NoTheme.class)
                .isPresent()) {
            return getLumoThemeDefinition().orElse(null);
        }

        return null;
    }

    private static boolean isInOSGi() {
        try {
            Class.forName("org.osgi.framework.FrameworkUtil");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static final class LazyLoadLumoTheme {

        private static final ThemeDefinition LUMO_CLASS_IF_AVAILABLE = loadLumoClassIfAvailable();

        /**
         * Loads the Lumo theme class from the classpath if it is available.
         *
         * @return the Lumo ThemeDefinition, or <code>null</code> if it is not
         *         available in the classpath
         */
        private static ThemeDefinition loadLumoClassIfAvailable() {
            try {
                Class<? extends ThemeDefinition> theme = (Class<? extends ThemeDefinition>) Class
                        .forName(
                                "com.vaadin.flow.theme.lumo.LumoThemeDefinition");
                return ReflectTools.createInstance(theme);
            } catch (ClassNotFoundException e) {
                // ignore, the Lumo class is not available in the classpath
                Logger logger = LoggerFactory
                        .getLogger(RouteUtil.class.getName());
                logger.trace(
                        "Lumo theme is not present in the classpath. The application will not use any default theme.",
                        e);
            }
            return null;
        }
    }
}
