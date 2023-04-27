/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;

/**
 * Copies production bundle files from pre-compiled bundle JAR into a folder
 * where production bundle is normally located.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.1
 */
public class TaskCopyBundleFiles implements FallibleCommand {

    private final Options options;

    public TaskCopyBundleFiles(Options options) {
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        URL statsJson = BundleValidationUtil
                .getProdBundleResource("config/stats.json");
        if (statsJson == null) {
            throw new IllegalStateException(
                    "Could not copy production bundle files, because couldn't find production bundle in the class-path");
        }
        String pathToJar = statsJson.getPath();
        int index = pathToJar.lastIndexOf(".jar!/");
        if (index >= 0) {
            // exclude relative path starting from !/
            pathToJar = pathToJar.substring(0, index + 4);
        }
        try {
            URI jarUri = new URI(pathToJar);
            JarContentsManager jarContentsManager = new JarContentsManager();
            jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(
                    new File(jarUri), Constants.PROD_BUNDLE_NAME,
                    options.getResourceOutputDirectory(), "**/*.*");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
