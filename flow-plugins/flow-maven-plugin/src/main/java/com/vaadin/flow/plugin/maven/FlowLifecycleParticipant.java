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
package com.vaadin.flow.plugin.maven;

import java.io.File;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven lifecycle participant that deletes the production
 * {@code flow-build-info.json} token file after each Maven session ends.
 * <p>
 * This supplements the {@code deleteOnExit()} call in
 * {@code BuildFrontendUtil}, which only fires on JVM exit and therefore never
 * runs when using the Maven Daemon (mvnd). By hooking into
 * {@link #afterSessionEnd(MavenSession)}, the token file is cleaned up after
 * every build invocation even when the daemon JVM stays alive between builds.
 * <p>
 * The exact token file path is written into the Maven project properties by
 * {@code BuildFrontendMojo} under {@link #TOKEN_FILE_PATH_PROPERTY} so that a
 * user-configured {@code resourceOutputDirectory} is always respected.
 * <p>
 * This participant is only active when the plugin is declared with
 * {@code <extensions>true</extensions>} in the user's {@code pom.xml}.
 * <p>
 * The component is registered via {@code META-INF/plexus/components.xml} rather
 * than annotations, since the {@code plexus-component-metadata} annotation
 * processor is not part of this build.
 */
public class FlowLifecycleParticipant
        extends AbstractMavenLifecycleParticipant {

    /**
     * Maven project property key under which {@code BuildFrontendMojo} stores
     * the absolute path of the token file it wrote. Read by
     * {@link #afterSessionEnd(MavenSession)} to delete the correct file.
     */
    public static final String TOKEN_FILE_PATH_PROPERTY = "vaadin.flow.tokenFilePath";

    private static final Logger logger = LoggerFactory
            .getLogger(FlowLifecycleParticipant.class);

    @Override
    public void afterSessionEnd(MavenSession session) {
        for (MavenProject project : session.getProjects()) {
            String tokenFilePath = project.getProperties()
                    .getProperty(TOKEN_FILE_PATH_PROPERTY);
            if (tokenFilePath == null) {
                // build-frontend did not run in this session; nothing to clean
                // up
                continue;
            }
            File tokenFile = new File(tokenFilePath);
            if (tokenFile.exists()) {
                if (tokenFile.delete()) {
                    logger.debug("Deleted flow-build-info.json from {}",
                            tokenFilePath);
                } else {
                    logger.debug(
                            "Failed to delete flow-build-info.json from {}",
                            tokenFilePath);
                }
            }
        }
    }
}
