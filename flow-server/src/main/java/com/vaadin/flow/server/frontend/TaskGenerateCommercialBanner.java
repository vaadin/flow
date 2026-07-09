/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>commercial-banner.js</code> if it is missing in
 * frontend/generated folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.9
 */
public class TaskGenerateCommercialBanner extends AbstractTaskClientGenerator {

    private Options options;

    /**
     * Create a task to generate <code>commercial-banner.js</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateCommercialBanner(Options options) {
        this.options = options;
    }

    @Override
    protected File getGeneratedFile() {
        return new File(
                new File(options.getFrontendDirectory(),
                        FrontendUtils.GENERATED),
                FrontendUtils.COMMERCIAL_BANNER_JS);
    }

    @Override
    protected boolean shouldGenerate() {
        return options.isProductionMode() && options.isBundleBuild()
                && options.isCommercialBannerEnabled();
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream content = getClass()
                .getResourceAsStream(FrontendUtils.COMMERCIAL_BANNER_JS)) {
            return IOUtils.toString(content, UTF_8);
        }
    }

}
