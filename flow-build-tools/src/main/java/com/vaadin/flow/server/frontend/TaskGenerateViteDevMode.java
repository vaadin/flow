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

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.StringUtil;

/**
 * Generate <code>vite-devmode.ts</code> if it is missing in frontend/generated
 * folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public class TaskGenerateViteDevMode extends AbstractTaskClientGenerator {

    private Options options;

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateViteDevMode(Options options) {
        this.options = options;
    }

    @Override
    protected File getGeneratedFile() {
        return new File(
                new File(options.getFrontendDirectory(),
                        FrontendUtils.GENERATED),
                FrontendUtils.VITE_DEVMODE_TS);
    }

    @Override
    protected boolean shouldGenerate() {
        return options.isFrontendHotdeploy() || options.isBundleBuild();
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream devModeStream = getClass()
                .getResourceAsStream(FrontendUtils.VITE_DEVMODE_TS)) {
            return StringUtil.toUTF8String(devModeStream);
        }
    }

}
