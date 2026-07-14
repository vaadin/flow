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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.vaadin.flow.internal.StringUtil;

/**
 * Generates the checker-only <code>tsconfig-checker.json</code> into the build
 * directory.
 * <p>
 * The project's <code>tsconfig.json</code> is used by the bundler to compile
 * all TypeScript sources, so it must not exclude
 * <code>generated/jar-resources</code> (excluding those files there would stop
 * the bundler from transpiling them). Type checking, on the other hand, should
 * still skip add-on sources, which are outside the project developer's control.
 * This task generates a separate configuration that inherits the project
 * <code>tsconfig.json</code> and re-adds the jar-resources exclusion, used only
 * by the type checker (vite-plugin-checker).
 * <p>
 * The file is a build artifact: it lives in the build directory, is overwritten
 * on every run and is not meant to be committed.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskGenerateCheckerTsConfig extends AbstractTaskClientGenerator {

    static final String CHECKER_TSCONFIG_JSON = "tsconfig-checker.json";

    private final Options options;

    /**
     * Create a task to generate <code>tsconfig-checker.json</code>.
     *
     * @param options
     *            the task options
     */
    TaskGenerateCheckerTsConfig(Options options) {
        this.options = options;
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream stream = getClass()
                .getResourceAsStream(CHECKER_TSCONFIG_JSON)) {
            String config = StringUtil.toUTF8String(stream);

            String frontendPath = options.getNpmFolder().toPath()
                    .relativize(options.getFrontendDirectory().toPath())
                    .toString().replaceAll("\\\\", "/");
            config = config.replace("%FRONTEND%/",
                    frontendPath.isEmpty() ? "" : frontendPath + "/");
            config = config.replace("%FRONTEND%", frontendPath);
            return config;
        }
    }

    @Override
    protected File getGeneratedFile() {
        return new File(options.getBuildDirectory(), CHECKER_TSCONFIG_JSON);
    }

    @Override
    protected boolean shouldGenerate() {
        // Build artifact: always regenerate to stay in sync with the project
        // tsconfig.json and the frontend location.
        return true;
    }
}
