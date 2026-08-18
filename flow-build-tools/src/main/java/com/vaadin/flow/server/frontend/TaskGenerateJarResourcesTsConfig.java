/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.io.InputStream;
import java.nio.file.Path;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.StringUtil;

/**
 * Generates the <code>tsconfig.json</code> for the frontend sources that are
 * copied from add-on jars.
 * <p>
 * The project <code>tsconfig.json</code> excludes the folder those sources are
 * copied to, so that the type checking rules of the project are not enforced on
 * third party sources. Bundlers apply <code>compilerOptions</code> only from a
 * configuration that owns the file, and a file excluded from the project
 * configuration is owned by none of them, so add-on sources would be
 * transformed with no compiler options at all: TypeScript decorators would be
 * left as raw syntax that browsers cannot parse. The generated configuration
 * takes ownership of the folder and inherits the compiler options of the
 * project, while turning its type checking rules off for the add-on sources.
 * <p>
 * The folder is emptied both when add-on resources are copied into it and when
 * npm files are cleaned, so this task runs after both, and in any case before
 * the bundle is built.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class TaskGenerateJarResourcesTsConfig extends AbstractTaskClientGenerator {

    private static final String TEMPLATE = "jar-resources-tsconfig.json";
    private static final String PROJECT_TSCONFIG_PLACEHOLDER = "%PROJECT_TSCONFIG%";

    private final Options options;

    /**
     * Create a task to generate the <code>tsconfig.json</code> for the frontend
     * sources copied from add-on jars.
     *
     * @param options
     *            the task options
     */
    TaskGenerateJarResourcesTsConfig(Options options) {
        this.options = options;
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream template = getClass().getResourceAsStream(TEMPLATE)) {
            return StringUtil.toUTF8String(template).replace(
                    PROJECT_TSCONFIG_PLACEHOLDER,
                    relativeProjectTsConfigPath());
        }
    }

    @Override
    protected File getGeneratedFile() {
        return getTsConfigFile(options);
    }

    @Override
    protected boolean shouldGenerate() {
        return options.getJarFrontendResourcesFolder() != null;
    }

    /**
     * Gets the configuration file this task generates.
     * <p>
     * It extends the project <code>tsconfig.json</code>, so it cannot outlive
     * that file: a configuration whose <code>extends</code> does not resolve
     * makes bundlers and the TypeScript compiler fail outright.
     *
     * @param options
     *            the options of the project, which do not have to configure the
     *            folder for the frontend sources of add-ons explicitly
     * @return the configuration file of the frontend sources of add-ons
     */
    static File getTsConfigFile(Options options) {
        File jarResourcesFolder = options.getJarFrontendResourcesFolder();
        if (jarResourcesFolder == null) {
            jarResourcesFolder = FrontendUtils
                    .getJarResourcesFolder(options.getFrontendDirectory());
        }
        return new File(jarResourcesFolder, TaskGenerateTsConfig.TSCONFIG_JSON);
    }

    /**
     * Resolves the project <code>tsconfig.json</code> as a path relative to the
     * folder the add-on sources are copied to, as that folder is not at a fixed
     * depth below the project folder.
     */
    private String relativeProjectTsConfigPath() {
        Path from = options.getJarFrontendResourcesFolder().toPath()
                .toAbsolutePath().normalize();
        Path projectTsConfig = new File(options.getNpmFolder(),
                TaskGenerateTsConfig.TSCONFIG_JSON).toPath().toAbsolutePath()
                .normalize();
        return from.relativize(projectTsConfig).toString().replaceAll("\\\\",
                "/");
    }
}
