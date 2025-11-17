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
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TSX;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Updated <code>index.ts</code> if it imports Flow from an old location.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskUpdateOldIndexTs implements FallibleCommand {

    private final File frontendDirectory;

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskUpdateOldIndexTs(Options options) {
        this.frontendDirectory = options.getFrontendDirectory();
    }

    @Override
    public void execute() throws ExecutionFailedException {
        Arrays.asList(INDEX_TSX, INDEX_TS, INDEX_JS).stream()
                .map(type -> new File(frontendDirectory, type))
                .filter(File::exists).forEach(this::modifyImportsIfNeeded);
    }

    private void modifyImportsIfNeeded(File indexFile) {
        try {

            String content = FileUtils.readFileToString(indexFile, UTF_8);
            String updated = content.replaceFirst(
                    "(['\"])../target/frontend/generated-flow-imports",
                    "$1Frontend/generated/flow/generated-flow-imports.js");
            if (!updated.equals(content)) {
                FileUtils.write(indexFile, updated, UTF_8);
            }
        } catch (IOException e) {
            getLogger().error("Unable to read or update " + indexFile, e);
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

}
