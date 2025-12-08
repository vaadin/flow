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

import static com.vaadin.flow.server.frontend.FrontendUtils.TAILWIND_JS;

/**
 * Generate <code>tailwind.js</code> wrapper if it is missing in the generated
 * frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public class TaskGenerateTailwindJs extends AbstractTaskClientGenerator {

    private File tailwindJs;

    /**
     * Create a task to generate <code>tailwind.css</code> integration file.
     *
     * @param options
     *            the task options
     */
    TaskGenerateTailwindJs(Options options) {
        tailwindJs = new File(options.getFrontendGeneratedFolder(),
                TAILWIND_JS);
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream indexStream = getClass()
                .getResourceAsStream(TAILWIND_JS)) {
            var template = StringUtil.toUTF8String(indexStream);
            return template;
        }
    }

    @Override
    protected File getGeneratedFile() {
        return tailwindJs;
    }

    @Override
    protected boolean shouldGenerate() {
        return true;
    }
}
