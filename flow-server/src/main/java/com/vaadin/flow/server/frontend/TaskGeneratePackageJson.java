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

import java.io.IOException;
import java.io.UncheckedIOException;

import tools.jackson.databind.node.ObjectNode;

/**
 * Creates the <code>package.json</code> if missing.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskGeneratePackageJson extends NodeUpdater {

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param options
     *            build options
     */
    TaskGeneratePackageJson(Options options) {
        super(null, options);
    }

    @Override
    public void execute() {
        try {
            modified = false;
            ObjectNode mainContent = getPackageJson();
            modified = updateDefaultDependencies(mainContent);
            if (modified) {
                if (!mainContent.has("type") || !mainContent.get("type")
                        .asString().equals("module")) {
                    mainContent.put("type", "module");
                    log().info(
                            """
                                    Adding package.json type as module to enable ES6 modules which is now required.
                                    With this change sources need to use 'import' instead of 'require' for imports.
                                    """);
                }
                writePackageFile(mainContent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
