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
package com.vaadin.flow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.FrontendUtils;

@Route("com.vaadin.flow.StateView")
public class StateView extends Div {
    private static final Logger log = LoggerFactory.getLogger(StateView.class);
    protected static String ENABLED_SPAN = "enabled";
    protected static String REACT_SPAN = "react_added";

    public void StateView() {
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Span enabled = new Span("React enabled: " + getUI().get().getSession()
                .getService().getDeploymentConfiguration().isReactEnabled());
        enabled.setId(ENABLED_SPAN);

        File baseDir = attachEvent.getSession().getConfiguration()
                .getProjectFolder();
        if (baseDir == null) {
            baseDir = getBaseDir(attachEvent);
        }
        String packageJson = null;
        try {
            packageJson = Files
                    .readString(baseDir.toPath().resolve("package.json"));
            log.info("Read package.json from {}", baseDir.getPath());
        } catch (IOException e) {
            log.error("Could not read package.json", e);
            packageJson = "";
        }

        Span react = new Span("React found: " + packageJson.contains("react"));
        react.setId(REACT_SPAN);

        add(enabled, new Div(), react);
    }

    private File getBaseDir(AttachEvent attachEvent) {

        String folder = attachEvent.getSession().getConfiguration()
                .getStringProperty(FrontendUtils.PROJECT_BASEDIR, null);
        if (folder != null) {
            return new File(folder);
        }

        File projectFolder = FileIOUtils.getProjectFolderFromClasspath();
        if (projectFolder != null) {
            return projectFolder;
        }

        /*
         * Accept user.dir or cwd as a fallback only if the directory seems to
         * be a Maven or Gradle project. Check to avoid cluttering server
         * directories (see tickets #8249, #8403).
         */
        String baseDirCandidate = System.getProperty("user.dir", ".");
        Path path = Paths.get(baseDirCandidate);
        if (path.toFile().isDirectory()
                && (path.resolve("pom.xml").toFile().exists()
                        || path.resolve("build.gradle").toFile().exists())) {
            return path.toAbsolutePath().toFile();
        } else {
            throw new IllegalStateException(String.format(
                    "Failed to determine project directory for dev mode. "
                            + "Directory '%s' does not look like a Maven or "
                            + "Gradle project. Ensure that you have run the "
                            + "prepare-frontend Maven goal, which generates "
                            + "'flow-build-info.json', prior to deploying your "
                            + "application",
                    path.toString()));
        }
    }
}
