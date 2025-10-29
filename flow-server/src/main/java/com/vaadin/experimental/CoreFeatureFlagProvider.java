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
package com.vaadin.experimental;

import java.util.List;

/**
 * Provides core Flow framework feature flags.
 *
 * @since 25.0
 */
public class CoreFeatureFlagProvider implements FeatureFlagProvider {

    public static final Feature COLLABORATION_ENGINE_BACKEND = new Feature(
            "Collaboration Kit backend for clustering support",
            "collaborationEngineBackend",
            "https://github.com/vaadin/platform/issues/1988", true, null);

    public static final Feature FLOW_FULLSTACK_SIGNALS = new Feature(
            "Flow Full-stack Signals", "flowFullstackSignals",
            "https://github.com/vaadin/platform/issues/7373", true, null);

    public static final Feature ACCESSIBLE_DISABLED_BUTTONS = new Feature(
            "Accessible disabled buttons", "accessibleDisabledButtons",
            "https://github.com/vaadin/web-components/issues/4585", true, null);

    public static final Feature COMPONENT_STYLE_INJECTION = new Feature(
            "Enable theme component style injection", "themeComponentStyles",
            "https://github.com/vaadin/flow/issues/21608", true, null);

    public static final Feature COPILOT_EXPERIMENTAL = new Feature(
            "Copilot experimental features", "copilotExperimentalFeatures",
            "https://vaadin.com/docs/latest/tools/copilot", false, null);

    public static final Feature TAILWIND_CSS = new Feature(
            "Tailwind CSS framework", "tailwindCss",
            "https://github.com/vaadin/flow/issues/21643", true, null);

    @Override
    public List<Feature> getFeatures() {
        return List.of(COLLABORATION_ENGINE_BACKEND, FLOW_FULLSTACK_SIGNALS,
                ACCESSIBLE_DISABLED_BUTTONS, COMPONENT_STYLE_INJECTION,
                COPILOT_EXPERIMENTAL, TAILWIND_CSS);
    }
}
