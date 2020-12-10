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

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Task for generating the theme-generated.js file for importing application
 * theme.
 *
 * @since
 */
public class TaskUpdateThemeImport implements FallibleCommand {

    private File themeImportFile;
    private ThemeDefinition theme;

    TaskUpdateThemeImport(File npmFolder, ThemeDefinition theme) {
        File generatedDir = new File(npmFolder, FrontendUtils.DEFAULT_GENERATED_DIR);
        this.themeImportFile = new File(new File(generatedDir, "theme"),
            "theme-generated.js");
        this.theme = theme;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (theme == null || theme.getName().isEmpty()) {
            return;
        }
        if (!themeImportFile.getParentFile().mkdirs()) {
            LoggerFactory.getLogger(getClass()).debug(
                "Didn't create folders as they probably already exist. "
                    + "If there is a problem check access rights for folder {}",
                themeImportFile.getParentFile().getAbsolutePath());
        }

        try {
            FileUtils.write(themeImportFile, String.format(
                "import {applyTheme as _applyTheme} from 'theme/%s/%s.generated.js';%n"
                    + "export const applyTheme = _applyTheme;%n",
                theme.getName(), theme.getName()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExecutionFailedException(
                "Unable to write theme import file", e);
        }
    }
}
