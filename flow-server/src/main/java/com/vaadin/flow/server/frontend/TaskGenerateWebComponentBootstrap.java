/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

        String generatedImportsRelativePath = FrontendUtils.getUnixRelativePath(
                frontendGeneratedDirectory.toPath(), generatedImports.toPath());

        lines.add(String.format("import '%s';", generatedImportsRelativePath));
        lines.add("import { init } from '" + FrontendUtils.JAR_RESOURCES_IMPORT
                + "FlowClient.js';");
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
