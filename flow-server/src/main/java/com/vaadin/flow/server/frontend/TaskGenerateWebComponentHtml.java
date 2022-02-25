/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import org.apache.commons.io.IOUtils;

import static com.vaadin.flow.server.frontend.FrontendUtils.WEB_COMPONENT_HTML;
import static java.nio.charset.StandardCharsets.UTF_8;

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
     * Create a task to generate <code>web-component.html</code> if necessary.
     *
     * @param frontendDirectory
     *            frontend directory is to check if the file already exists
     *            there.
     */
    TaskGenerateWebComponentHtml(File frontendDirectory) {
        webComponentHtml = new File(frontendDirectory, WEB_COMPONENT_HTML);
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream indexStream = getClass()
                .getResourceAsStream(WEB_COMPONENT_HTML)) {
            return IOUtils.toString(indexStream, UTF_8);
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
