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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;

/**
 * Builder class for {@link InvocationRequest} objects.
 * <p>
 * When a {@link MavenSession} is given, the nested Maven execution inherits the
 * configuration of the running one: offline mode, settings and toolchains
 * files, local repository, profiles, user properties and batch mode. Without
 * these, the nested execution would, for example, use the default local
 * repository and check artifacts against remote repositories although the outer
 * build runs offline.
 */
class InvocationRequestBuilder {

    private String groupId;
    private String artifactId;
    private String version;
    private String goal;
    private MavenSession session;
    private Properties properties;

    InvocationRequestBuilder() {
    }

    InvocationRequestBuilder groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    InvocationRequestBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    InvocationRequestBuilder version(String version) {
        this.version = version;
        return this;
    }

    InvocationRequestBuilder goal(String goal) {
        this.goal = goal;
        return this;
    }

    /**
     * Sets the Maven session whose configuration the nested execution inherits.
     *
     * @param session
     *            the running Maven session, or {@code null} to not inherit
     *            anything
     * @return this builder
     */
    InvocationRequestBuilder session(MavenSession session) {
        this.session = session;
        return this;
    }

    /**
     * Sets the properties passed to the nested execution. They take precedence
     * over the user properties inherited from the session.
     *
     * @param properties
     *            the properties for the nested execution
     * @return this builder
     */
    InvocationRequestBuilder properties(Properties properties) {
        this.properties = new Properties();
        if (properties != null) {
            this.properties.putAll(properties);
        }
        return this;
    }

    InvocationRequest createInvocationRequest() {
        Objects.requireNonNull(groupId, "groupId is required");
        Objects.requireNonNull(artifactId, "artifactId is required");
        Objects.requireNonNull(version, "version is required");
        Objects.requireNonNull(goal, "goal is required");
        InvocationRequest request = new DefaultInvocationRequest()
                .setGoals(Collections.singletonList(String.format("%s:%s:%s:%s",
                        groupId, artifactId, version, goal)));

        Properties requestProperties = new Properties();
        if (session != null) {
            inheritSessionConfiguration(request, session.getRequest());
            requestProperties.putAll(session.getUserProperties());
        }
        if (properties != null) {
            requestProperties.putAll(properties);
        }
        request.setProperties(requestProperties);
        return request;
    }

    private static void inheritSessionConfiguration(InvocationRequest request,
            MavenExecutionRequest executionRequest) {
        request.setOffline(executionRequest.isOffline());
        request.setBatchMode(!executionRequest.isInteractiveMode());
        request.setLocalRepositoryDirectory(
                executionRequest.getLocalRepositoryPath());
        // Maven fails when a given settings or toolchains file does not
        // exist, and the session reports the default locations even when
        // nothing is there
        request.setUserSettingsFile(
                existingFile(executionRequest.getUserSettingsFile()));
        request.setGlobalSettingsFile(
                existingFile(executionRequest.getGlobalSettingsFile()));
        request.setToolchainsFile(
                existingFile(executionRequest.getUserToolchainsFile()));
        request.setGlobalToolchainsFile(
                existingFile(executionRequest.getGlobalToolchainsFile()));

        List<String> profiles = new ArrayList<>(
                executionRequest.getActiveProfiles());
        executionRequest.getInactiveProfiles()
                .forEach(profile -> profiles.add("!" + profile));
        request.setProfiles(profiles);
    }

    private static File existingFile(File file) {
        return file != null && file.isFile() ? file : null;
    }

}
