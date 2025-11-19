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

import static com.vaadin.flow.server.frontend.FrontendUtils.WEB_COMPONENT_HTML;

/**
 * Generate <code>web-component.html</code> if it is missing in frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateWebComponentHtml extends AbstractTaskClientGenerator {

    private File webComponentHtml;

    /**
     * Create a task to generate <code>web-component.html</code> in the frontend
     * directory if necessary.
     *
     * @param options
     *            frontend directory is to check if the file already exists
     *            there.
     */
    TaskGenerateWebComponentHtml(Options options) {
        // The user is generally not supposed to modify web-component.html and
        // therefore you might have expected it to be generated
        // in the frontend generated directory.
        // It is however generated in the frontend directory like index.html
        // since it is easier to serve it from there in terms of the Vite
        // config.
        webComponentHtml = new File(options.getFrontendDirectory(),
                WEB_COMPONENT_HTML);
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream indexStream = getClass()
                .getResourceAsStream(WEB_COMPONENT_HTML)) {
            return StringUtil.toUtf8Str(indexStream);
        }
    }

    @Override
    protected File getGeneratedFile() {
        return webComponentHtml;
    }

    @Override
    protected boolean shouldGenerate() {
        return !webComponentHtml.exists();
    }
}
