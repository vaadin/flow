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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.frontend.FrontendUtils.BOOTSTRAP_FILE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.FEATURE_FLAGS_FILE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TSX;

/**
 * A task for generating the bootstrap file
 * {@link FrontendUtils#BOOTSTRAP_FILE_NAME} during `package` Maven goal.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
public class TaskGenerateBootstrap extends AbstractTaskClientGenerator {

    static final String DEV_TOOLS_IMPORT = String.format(
            "import '%svaadin-dev-tools.js';%n",
            FrontendUtils.JAR_RESOURCES_IMPORT + "vaadin-dev-tools/");
    private final FrontendDependenciesScanner frontDeps;
    private final Options options;
    private List<TypeScriptBootstrapModifier> modifiers;

    TaskGenerateBootstrap(FrontendDependenciesScanner frontDeps,
            Options options) {
        this.frontDeps = frontDeps;
        this.options = options;
        this.modifiers = new ArrayList<>();
        for (Class<? extends TypeScriptBootstrapModifier> modifierClass : options
                .getClassFinder()
                .getSubTypesOf(TypeScriptBootstrapModifier.class)) {
            try {
                this.modifiers
                        .add(modifierClass.getConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                LoggerFactory.getLogger(TaskGenerateBootstrap.class).error(
                        "Failed to instantiate TypeScriptBootstrapModifier", e);
            }
        }
    }

    @Override
    protected String getFileContent() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("import '%s';%n", getIndexTsEntryPath()));
        if (options.isReactEnabled()) {
            lines.add("import './vaadin-react.js';");
        }
        if (!options.isProductionMode()) {
            lines.add(DEV_TOOLS_IMPORT);
        }
        lines.addAll(getThemeLines());

        for (TypeScriptBootstrapModifier modifier : modifiers) {
            modifier.modify(lines, options, frontDeps);
        }
        lines.add(0,
                String.format("import './%s';%n", FEATURE_FLAGS_FILE_NAME));
        return String.join(System.lineSeparator(), lines);
    }

    @Override
    protected File getGeneratedFile() {
        File frontendGeneratedDirectory = new File(
                options.getFrontendDirectory(), GENERATED);
        return new File(frontendGeneratedDirectory, BOOTSTRAP_FILE_NAME);
    }

    @Override
    protected boolean shouldGenerate() {
        return frontDeps != null;
    }

    private String getIndexTsEntryPath() {
        File frontendDirectory = options.getFrontendDirectory();
        boolean hasCustomIndexFile = new File(frontendDirectory, INDEX_TS)
                .exists() || new File(frontendDirectory, INDEX_JS).exists()
                || new File(frontendDirectory, INDEX_TSX).exists();
        if (hasCustomIndexFile) {
            return "../index";
        } else {
            return "./index";
        }
    }

    private Collection<String> getThemeLines() {
        Collection<String> lines = new ArrayList<>();
        ThemeDefinition themeDef = frontDeps.getThemeDefinition();
        if (themeDef != null && !"".equals(themeDef.getName())) {
            lines.add("import './theme-" + themeDef.getName()
                    + ".global.generated.js';");
            lines.add("import { applyTheme } from './theme.js';");
            lines.add("applyTheme(document);");
            lines.add("");
        } else {
            lines.add("import './css.generated.js';");
            lines.add("import { applyCss } from './css.generated.js';");
            lines.add("applyCss(document);");
            lines.add("");
        }
        return lines;
    }

}
