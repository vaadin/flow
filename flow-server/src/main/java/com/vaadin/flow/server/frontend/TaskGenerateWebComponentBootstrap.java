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
import java.util.ArrayList;
import java.util.List;

import static com.vaadin.flow.server.frontend.FrontendUtils.WEB_COMPONENT_BOOTSTRAP_FILE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;

/**
 * A task for generating the bootstrap file for exported web components
 * {@link FrontendUtils#WEB_COMPONENT_BOOTSTRAP_FILE_NAME} during `package`
 * Maven goal.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
public class TaskGenerateWebComponentBootstrap
        extends AbstractTaskClientGenerator {

    private final File frontendGeneratedDirectory;
    private final File generatedImports;

    /**
     * Create a task to generate <code>vaadin-web-component.ts</code> if
     * necessary.
     *
     * @param frontendDirectory
     *            frontend directory is to check if the file already exists
     *            there.
     * @param generatedImports
     *            the flow generated imports file to include in the
     *            <code>vaadin-web-component.ts</code>
     */
    TaskGenerateWebComponentBootstrap(File frontendDirectory,
            File generatedImports) {
        this.frontendGeneratedDirectory = new File(frontendDirectory,
                GENERATED);
        this.generatedImports = generatedImports;
    }

    @Override
    protected String getFileContent() {
        List<String> lines = new ArrayList<>();

        String generatedImportsRelativePath = FrontendUtils
                .getUnixRelativePath(frontendGeneratedDirectory.toPath(),
                        generatedImports.toPath())
                .replaceFirst("\\.(js|ts)$", "");

        lines.add(String.format("import '%s';", generatedImportsRelativePath));
        lines.add("import { init } from '@vaadin/flow-frontend/FlowClient';");
        lines.add("init();");

        return String.join("\n", lines);
    }

    @Override
    protected File getGeneratedFile() {
        return new File(frontendGeneratedDirectory,
                WEB_COMPONENT_BOOTSTRAP_FILE_NAME);
    }

    @Override
    protected boolean shouldGenerate() {
        return true;
    }
}
