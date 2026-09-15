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
package com.vaadin.quarkus.deployment.vaadinplugin;

import java.nio.file.Path;
import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workaround to collect workspace info when it is not available at build time.
 * <p>
 * During a normal build, workspace information is available only if the
 * {@code quarkus.bootstrap.workspace-discovery} property is set to
 * {@code true}. This class gets executed during the code generation phase, so
 * it has access to the workspace details and can persist them to make them
 * available to subsequent build steps.
 */
public class WorkspaceInfoCollector implements CodeGenProvider {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(WorkspaceInfoCollector.class);
    private WorkspaceModule module;

    @Override
    public String providerId() {
        return "vaadin-plugin-workspace-info";
    }

    @Override
    public String inputDirectory() {
        return "";
    }

    @Override
    public void init(ApplicationModel model, Map<String, String> properties) {
        module = model.getApplicationModule();
    }

    @Override
    public boolean shouldRun(Path sourceDir, Config config) {
        if (module == null) {
            LOGGER.debug(
                    "Workspace information for Vaadin embedded plugin not collected because module details are not available");
            return false;
        }
        if (!config.getOptionalValue("vaadin.build.enabled", Boolean.class)
                .orElse(true)) {
            LOGGER.info(
                    "Workspace information for Vaadin embedded plugin not collected because Vaadin embedded plugin is disabled");
            return false;
        }
        if (config.getOptionalValue("quarkus.bootstrap.workspace-discovery",
                Boolean.class).orElse(false)) {
            LOGGER.debug(
                    "Workspace information for Vaadin embedded plugin not collected because workspace information already available at build time");
            return false;
        }
        return true;
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        if (module != null) {
            LOGGER.info("Collecting workspace information for Vaadin plugin");
            try {
                WorkspaceInfo.save(module, context.workDir());
            } catch (Exception e) {
                throw new CodeGenException(
                        "Failed to store workspace information for Vaadin plugin",
                        e);
            }
        }
        return false;
    }

}
