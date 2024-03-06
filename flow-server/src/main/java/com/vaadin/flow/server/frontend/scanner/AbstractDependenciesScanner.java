/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.AbstractTheme;

/**
 * Common scanner functionality.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
abstract class AbstractDependenciesScanner
        implements FrontendDependenciesScanner {

    public static final String LUMO = "com.vaadin.flow.theme.lumo.Lumo";

    protected static final String ERROR_INVALID_PWA_ANNOTATION = "There can only be one @PWA annotation and it must be set on the "
            + AppShellConfigurator.class.getSimpleName() + " implementor.";

    private final ClassFinder finder;
    private final FeatureFlags featureFlags;

    protected AbstractDependenciesScanner(ClassFinder finder,
            FeatureFlags featureFlags) {
        this.finder = finder;
        this.featureFlags = featureFlags;
    }

    protected final ClassFinder getFinder() {
        return finder;
    }

    protected final boolean isExperimental(String className) {
        return featureFlags != null && featureFlags.getFeatures().stream()
                .anyMatch(f -> !f.isEnabled()
                        && className.equals(f.getComponentClassName()));
    }

    protected Class<? extends AbstractTheme> getLumoTheme() {
        try {
            return finder.loadClass(LUMO);
        } catch (ClassNotFoundException ignore) { // NOSONAR
            return null;
        }
    }
}
