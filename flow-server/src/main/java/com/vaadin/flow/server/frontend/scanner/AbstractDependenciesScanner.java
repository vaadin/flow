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

import com.vaadin.flow.theme.AbstractTheme;

/**
 * Common scanner functionality.
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
