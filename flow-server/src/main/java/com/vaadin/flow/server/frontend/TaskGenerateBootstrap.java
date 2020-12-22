/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * A task for generate a TS file which is always executed in a Vaadin app
 *
 * @author Vaadin Ltd
 */
public class TaskGenerateBootstrap implements FallibleCommand {

    private final FrontendDependenciesScanner frontDeps;
    private final File outputFolder;

    private static final String CUSTOM_BOOTSTRAP_FILE_NAME = "vaadin.ts";

    TaskGenerateBootstrap(FrontendDependenciesScanner frontDeps,
            File outputFolder) {
        this.frontDeps = frontDeps;
        this.outputFolder = outputFolder;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (!shouldGenerateThemeScript()) {
            return;
        }

        File bootstrapFile = new File(outputFolder, CUSTOM_BOOTSTRAP_FILE_NAME);
        Collection<String> lines = new ArrayList<>(getThemeLines());

        try {
            FileUtils.writeStringToFile(bootstrapFile, String.join("\n", lines),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to read " + bootstrapFile.getName(), e);
        }
    }

    private Collection<String> getThemeLines() {
        Collection<String> lines = new ArrayList<>();
        lines.add("//@ts-ignore");
        lines.add("import {applyTheme} from '../../target/flow-frontend/themes/theme-generated.js';");
        lines.add("applyTheme(document);");
        return lines;
    }

    private boolean shouldGenerateThemeScript() {
        ThemeDefinition themeDef = frontDeps.getThemeDefinition();
        return themeDef != null && !"".equals(themeDef.getName());
    }
}
