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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.frontend.FrontendUtils.FEATURE_FLAGS_FILE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEB_COMPONENT_BOOTSTRAP_FILE_NAME;

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
    private Options options;

    /**
     * Create a task to generate <code>vaadin-web-component.ts</code> if
     * necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateWebComponentBootstrap(Options options) {
        this.frontendGeneratedDirectory = new File(
                options.getFrontendDirectory(), GENERATED);
        this.options = options;
    }

    @Override
    protected String getFileContent() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("import './%s';%n", FEATURE_FLAGS_FILE_NAME));
        lines.add("import 'Frontend/generated/flow/"
                + FrontendUtils.IMPORTS_WEB_COMPONENT_NAME + "';");
        lines.add("import { init } from '" + FrontendUtils.JAR_RESOURCES_IMPORT
                + "FlowClient.js';");
        lines.add("init();");

        applyCssImportWhenNoTheme(lines);
        return String.join("\n", lines);
    }

    private void applyCssImportWhenNoTheme(List<String> lines) {
        ThemeDefinition themeDefinition = options
                .getFrontendDependenciesScanner().getThemeDefinition();

        // If no theme available add applyTheme for css import
        if (themeDefinition == null || "".equals(themeDefinition.getName())) {
            lines.add("import './css.generated.js';");
            lines.add("import { applyCss } from './css.generated.js';");
            lines.add("applyCss(document);");
            lines.add("");
        }
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
