/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

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

    private final ClassFinder finder;

    protected AbstractDependenciesScanner(ClassFinder finder) {
        this.finder = finder;
    }

    protected final ClassFinder getFinder() {
        return finder;
    }

    protected Class<? extends AbstractTheme> getLumoTheme() {
        try {
            return finder.loadClass(LUMO);
        } catch (ClassNotFoundException ignore) { // NOSONAR
            return null;
        }
    }
}
