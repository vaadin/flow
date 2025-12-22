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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.experimental.Feature;
import com.vaadin.flow.internal.FrontendUtils;

import static com.vaadin.flow.internal.FrontendUtils.FEATURE_FLAGS_FILE_NAME;
import static com.vaadin.flow.internal.FrontendUtils.GENERATED;

/**
 * A task for generating the feature flags file
 * {@link FrontendUtils#FEATURE_FLAGS_FILE_NAME} during `package` Maven goal.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
public class TaskGenerateFeatureFlags extends AbstractTaskClientGenerator {

    private final Options options;

    TaskGenerateFeatureFlags(Options options) {
        this.options = options;
    }

    @Override
    protected String getFileContent() {
        List<String> lines = new ArrayList<>();
        lines.add("// @ts-nocheck");
        lines.add("window.Vaadin = window.Vaadin || {};");
        lines.add(
                "window.Vaadin.featureFlags = window.Vaadin.featureFlags || {};");

        // Initialize the flag entries only once. For exported web-components,
        // this script may be executed multiple times (one per embedded
        // component) and we should prevent active flags get overridden.
        List<Feature> featureFlags = options.getFeatureFlags().getFeatures();
        if (!featureFlags.isEmpty()) {
            lines.add(
                    "if (Object.keys(window.Vaadin.featureFlags).length === 0) {");
            featureFlags.forEach(feature -> {
                lines.add(
                        String.format("window.Vaadin.featureFlags.%s = false;",
                                feature.getId()));
            });
            lines.add("};");
        }

        // Multiple feature flags updater functions can be registered, in case
        // of exported web-component. If the component comes from different web
        // applications, the active flags might not be the same.
        lines.add("if (window.Vaadin.featureFlagsUpdaters) { ");
        lines.add(
                "const activator = (id) => window.Vaadin.featureFlags[id] = true;");
        lines.add(
                "window.Vaadin.featureFlagsUpdaters.forEach(updater => updater(activator));");
        lines.add("delete window.Vaadin.featureFlagsUpdaters;");
        lines.add("} ");

        // See https://github.com/vaadin/flow/issues/14184
        lines.add("export {};");

        return String.join(System.lineSeparator(), lines);
    }

    @Override
    protected File getGeneratedFile() {
        File frontendGeneratedDirectory = new File(
                options.getFrontendDirectory(), GENERATED);
        return new File(frontendGeneratedDirectory, FEATURE_FLAGS_FILE_NAME);
    }

    @Override
    protected boolean shouldGenerate() {
        return true;
    }
}
