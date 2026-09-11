/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.dependencies;

public class TestVersion {

    /**
     * Version of {@code @vaadin/vaadin-themable-mixin}, the only Vaadin npm
     * package the tests are allowed to depend on. It is needed to resolve the
     * {@code register-styles} import that Flow generates for per-component
     * theme CSS; everything else is covered by faux elements, so no other
     * Vaadin npm package should be added here.
     * <p>
     * This is deliberately pinned to a stable release rather than to the
     * version of the platform currently in development. The mixin changes very
     * rarely and the tests only use {@code ThemableMixin} and
     * {@code registerStyles}, so there is no need to keep this in step with
     * every platform bump — update it only if a test actually needs newer
     * behaviour.
     */
    public static final String THEMABLE_MIXIN = "25.2.9";
    public static final String FONTAWESOME = "5.15.1";

}
