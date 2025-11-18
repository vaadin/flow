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
import java.io.IOException;
import java.io.InputStream;

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
            return new String(content.readAllBytes());
        }
    }

}
