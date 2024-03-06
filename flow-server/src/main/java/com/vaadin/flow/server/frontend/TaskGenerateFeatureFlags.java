/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import com.vaadin.experimental.FeatureFlags;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.vaadin.flow.server.frontend.FrontendUtils.*;

/**
 * A task for generating the feature flags file
 * {@link FrontendUtils#FEATURE_FLAGS_FILE_NAME} during `package` Maven goal.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
public class TaskGenerateFeatureFlags extends AbstractTaskClientGenerator {

    private final File frontendGeneratedDirectory;
    private final FeatureFlags featureFlags;

    TaskGenerateFeatureFlags(File frontendDirectory,
            FeatureFlags featureFlags) {
        this.frontendGeneratedDirectory = new File(frontendDirectory,
                GENERATED);
        this.featureFlags = featureFlags;
    }

    @Override
    protected String getFileContent() {
        List<String> lines = new ArrayList<>();
        lines.add("// @ts-nocheck");
        lines.add("window.Vaadin = window.Vaadin || {};");
        lines.add(
                "window.Vaadin.featureFlags = window.Vaadin.featureFlags || {};");

        featureFlags.getFeatures().forEach(feature -> {
            lines.add(String.format("window.Vaadin.featureFlags.%s = %s;",
                    feature.getId(), featureFlags.isEnabled(feature)));
        });

        // See https://github.com/vaadin/flow/issues/14184
        lines.add("export {};");

        return String.join(System.lineSeparator(), lines);
    }

    @Override
    protected File getGeneratedFile() {
        return new File(frontendGeneratedDirectory, FEATURE_FLAGS_FILE_NAME);
    }

    @Override
    protected boolean shouldGenerate() {
        return true;
    }
}
