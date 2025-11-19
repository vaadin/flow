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

import com.vaadin.flow.internal.StringUtil;

import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC_JS;

/**
 * Generate <code>index.html</code> if it is missing in frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateServiceWorker extends AbstractTaskClientGenerator {

    private Options options;

    /**
     * Create a task to generate <code>sw.ts</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateServiceWorker(Options options) {
        this.options = options;
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream swStream = getClass()
                .getResourceAsStream(SERVICE_WORKER_SRC)) {
            return StringUtil.toUtf8Str(swStream);
        }
    }

    @Override
    protected File getGeneratedFile() {
        return new File(options.getBuildDirectory(), SERVICE_WORKER_SRC);
    }

    @Override
    protected boolean shouldGenerate() {
        File serviceWorker = new File(options.getFrontendDirectory(),
                SERVICE_WORKER_SRC);
        File serviceWorkerJs = new File(options.getFrontendDirectory(),
                SERVICE_WORKER_SRC_JS);
        return !serviceWorker.exists() && !serviceWorkerJs.exists();
    }
}
