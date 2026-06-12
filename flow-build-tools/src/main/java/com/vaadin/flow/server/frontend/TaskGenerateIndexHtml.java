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

import static com.vaadin.flow.internal.FrontendUtils.INDEX_HTML;

/**
 * Generate the default <code>index.html</code> into the frontend
 * <code>generated/</code> folder so the file is not committed to source
 * control. A user-provided <code>index.html</code> placed directly in the
 * frontend folder overrides it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateIndexHtml extends AbstractTaskClientGenerator {

    private final File userIndexHtml;
    private final File generatedIndexHtml;

    /**
     * Create a task to generate <code>index.html</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateIndexHtml(Options options) {
        userIndexHtml = new File(options.getFrontendDirectory(), INDEX_HTML);
        generatedIndexHtml = new File(options.getFrontendGeneratedFolder(),
                INDEX_HTML);
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream indexStream = getClass()
                .getResourceAsStream(INDEX_HTML)) {
            return StringUtil.toUTF8String(indexStream);
        }
    }

    @Override
    protected File getGeneratedFile() {
        return generatedIndexHtml;
    }

    @Override
    protected boolean shouldGenerate() {
        // Skip writing the default into the generated folder when the user
        // has provided their own index.html in the frontend folder.
        return !userIndexHtml.exists();
    }
}
