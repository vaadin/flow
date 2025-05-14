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
package com.vaadin.flow.server;

/**
 * The mode the application is running in.
 *
 * One of production, development using livereload or development using bundle
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
