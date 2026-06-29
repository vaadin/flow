/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

/**
 * The mode the application is running in.
 *
 * One of production, development using livereload or development using bundle
 *
 * @since 24.0.1
 */
public enum Mode {
    PRODUCTION_CUSTOM("production", true),
    PRODUCTION_PRECOMPILED_BUNDLE("production", true),
    DEVELOPMENT_FRONTEND_LIVERELOAD("development", false),
    DEVELOPMENT_BUNDLE("development", false);

    private final String name;
    private final boolean production;

    Mode(String name, boolean production) {
        this.name = name;
        this.production = production;
    }

    public boolean isProduction() {
        return production;
    }

    @Override
    public String toString() {
        return name;
    }
}
