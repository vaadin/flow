package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.theme.ThemeDefinition;

import org.apache.commons.io.FileUtils;

public class TaskUpdateThemeImport implements FallibleCommand {

    private File themeImportFile;
    private ThemeDefinition theme;

    protected TaskUpdateThemeImport(File npmFolder, File generatedDirectory, ThemeDefinition theme) {
        File nodeModules = new File(npmFolder, FrontendUtils.NODE_MODULES);
        File flowFrontend = new File(nodeModules, FrontendUtils.FLOW_NPM_PACKAGE_NAME);
        this.themeImportFile = new File(new File(flowFrontend, "theme"), "theme.js");
        this.theme = theme;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (theme.getName().isEmpty()) {
            return;
        }
        themeImportFile.getParentFile().mkdirs();

        try {
            FileUtils.write(
                    themeImportFile, "import {applyTheme as _applyTheme} from './" + theme.getName() + "/"
                            + theme.getName() + ".js';\nexport const applyTheme = _applyTheme;\n",
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExecutionFailedException("Unable to write theme import file", e);
        }

    }

}
