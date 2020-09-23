package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.server.ExecutionFailedException;

import org.apache.commons.io.FileUtils;

public class TaskUpdateApplicationThemeImport implements FallibleCommand {

    private File themeImportFile;
    private String applicationTheme;

    protected TaskUpdateApplicationThemeImport(File npmFolder, File generatedDirectory, String applicationTheme) {
        File nodeModules = new File(npmFolder, FrontendUtils.NODE_MODULES);
        File flowFrontend = new File(nodeModules, FrontendUtils.FLOW_NPM_PACKAGE_NAME);
        this.themeImportFile = new File(new File(flowFrontend, "theme"), "applicationTheme.js");
        this.applicationTheme = applicationTheme;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        themeImportFile.getParentFile().mkdirs();

        try {
            FileUtils.write(
                    themeImportFile, "import {applyTheme as _applyTheme} from './" + applicationTheme + "/"
                            + applicationTheme + ".js';\nexport const applyTheme = _applyTheme;\n",
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExecutionFailedException("Unable to write theme import file", e);
        }

    }

}
