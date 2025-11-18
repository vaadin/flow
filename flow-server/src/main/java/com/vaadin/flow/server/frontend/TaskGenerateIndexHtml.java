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

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_HTML;

/**
 * Generate <code>index.html</code> if it is missing in frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateIndexHtml extends AbstractTaskClientGenerator {

    private File indexHtml;

    /**
     * Create a task to generate <code>index.html</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateIndexHtml(Options options) {
        indexHtml = new File(options.getFrontendDirectory(), INDEX_HTML);
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream indexStream = getClass()
                .getResourceAsStream(INDEX_HTML)) {
            return new String(indexStream.readAllBytes());
        }
    }

    @Override
    protected File getGeneratedFile() {
        return indexHtml;
    }

    @Override
    protected boolean shouldGenerate() {
        return !indexHtml.exists();
    }
}
