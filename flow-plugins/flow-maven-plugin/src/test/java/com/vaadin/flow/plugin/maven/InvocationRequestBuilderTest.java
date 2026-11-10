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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvocationRequestBuilderTest {

    private final String groupId = "group.id";
    private final String artifactId = "artifact.id";
    private final String version = "1.0.1";
    private final String goal = "goal";

    @TempDir
    Path tempDir;

    @Test
    void createInvocationRequest() {
        InvocationRequestBuilder requestBuilder = new InvocationRequestBuilder();
        InvocationRequest request = requestBuilder.groupId(groupId)
                .artifactId(artifactId).version(version).goal(goal)
                .createInvocationRequest();
        List<String> goals = request.getGoals();
        assertEquals(1, goals.size());

        String expectedGoal = String.format("%s:%s:%s:%s", groupId, artifactId,
                version, goal);
        String actualGoal = goals.get(0);
        assertEquals(expectedGoal, actualGoal);
    }

    @Test
    void createInvocationRequest_inheritsSessionConfiguration()
            throws IOException {
        File localRepository = tempDir.resolve("repository").toFile();
        File userSettings = Files.createFile(tempDir.resolve("settings.xml"))
                .toFile();
        File toolchains = Files.createFile(tempDir.resolve("toolchains.xml"))
                .toFile();
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest()
                .setOffline(true).setInteractiveMode(false)
                .setLocalRepositoryPath(localRepository)
                .setUserSettingsFile(userSettings)
                .setGlobalSettingsFile(
                        tempDir.resolve("missing-settings.xml").toFile())
                .setUserToolchainsFile(toolchains)
                .setActiveProfiles(List.of("active"))
                .setInactiveProfiles(List.of("inactive"));
        Properties userProperties = new Properties();
        userProperties.setProperty("user.only", "user");
        userProperties.setProperty("overridden", "user");
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(executionRequest);
        when(session.getUserProperties()).thenReturn(userProperties);
        Properties properties = new Properties();
        properties.setProperty("overridden", "mojo");

        InvocationRequest request = new InvocationRequestBuilder()
                .groupId(groupId).artifactId(artifactId).version(version)
                .goal(goal).session(session).properties(properties)
                .createInvocationRequest();

        assertTrue(request.isOffline());
        assertTrue(request.isBatchMode());
        assertEquals(localRepository,
                request.getLocalRepositoryDirectory(null));
        assertEquals(userSettings, request.getUserSettingsFile());
        assertNull(request.getGlobalSettingsFile());
        assertEquals(toolchains, request.getToolchainsFile());
        assertEquals(List.of("active", "!inactive"), request.getProfiles());
        assertEquals("user", request.getProperties().getProperty("user.only"));
        assertEquals("mojo", request.getProperties().getProperty("overridden"));
    }

    @Test
    void createInvocationRequest_withoutSession_usesGivenProperties() {
        Properties properties = new Properties();
        properties.setProperty("key", "value");

        InvocationRequest request = new InvocationRequestBuilder()
                .groupId(groupId).artifactId(artifactId).version(version)
                .goal(goal).properties(properties).createInvocationRequest();

        assertFalse(request.isOffline());
        assertEquals(properties, request.getProperties());
    }

}
