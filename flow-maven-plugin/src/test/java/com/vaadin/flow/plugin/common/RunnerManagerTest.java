/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.plugin.common;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.frontend.FrontendToolsLocator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RunnerManagerTest {
    private final ProxyConfig proxyConfig = new ProxyConfig(
            Collections.emptyList());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder downloadDirectory = new TemporaryFolder();

    /**
     * Just to check if yarn execute task contains customized URL for npm
     * registry
     */
    @Test
    public void badNpmRegistryShouldThrowException()
            throws TaskRunnerException {
        File nonExistingFile = new File("doesNotExist");
        FrontendToolsLocator locatorMock = mock(FrontendToolsLocator.class);
        when(locatorMock.verifyTool(nonExistingFile)).thenReturn(true);

        String incorrectRegistry = "https://dummyhost:1234/";

        RunnerManager runnerManager = new RunnerManager.Builder(
                downloadDirectory.getRoot(), proxyConfig)
                        .localInstallations(nonExistingFile, nonExistingFile)
                        .autodetectTools(false)
                        .frontendToolsLocator(locatorMock)
                        .npmRegistryUrl(incorrectRegistry).build();

        exception.expect(TaskRunnerException.class);
        exception.expectMessage(
                String.format("--registry=%s", incorrectRegistry));
        runnerManager.getYarnRunner().execute(null, Collections.emptyMap());
    }

    @Test
    public void builderCreationThrowsWhenNoNodeDataIsSpecified_node() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("node");
        new RunnerManager.Builder(downloadDirectory.getRoot(), proxyConfig)
                .autodetectTools(false).build();
    }

    @Test
    public void builderCreationThrowsWhenNoNodeDataIsSpecified_yarn() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("yarn");
        new RunnerManager.Builder(downloadDirectory.getRoot(), proxyConfig)
                .versionsToDownload("1.1.1", null).autodetectTools(false)
                .build();
    }

    @Test
    public void locallyInstalledToolsUsed() {
        File nodePath = new File("node_local");
        File yarnPath = new File("yarn_local");

        FrontendToolsLocator locatorMock = mock(FrontendToolsLocator.class);
        when(locatorMock.verifyTool(nodePath)).thenReturn(true);
        when(locatorMock.verifyTool(yarnPath)).thenReturn(true);

        RunnerManager runnerManager = new RunnerManager.Builder(
                downloadDirectory.getRoot(), proxyConfig)
                        .localInstallations(nodePath, yarnPath)
                        .autodetectTools(false)
                        .frontendToolsLocator(locatorMock).build();

        assertNotNull("Frontend tool runners should be present",
                runnerManager.getGulpRunner());
        assertNotNull("Frontend tool runners should be present",
                runnerManager.getYarnRunner());
        assertArrayEquals(
                "No file should be downloaded when local tools are used",
                new String[] {}, downloadDirectory.getRoot().list());
        verify(locatorMock, times(1)).verifyTool(nodePath);
        verify(locatorMock, times(1)).verifyTool(yarnPath);
        verify(locatorMock, times(0)).tryLocateTool(anyString());
        verify(locatorMock, times(0)).tryLocateTool(anyString());
    }

    @Test
    public void automaticLocationIsPerformedForAbsentTools() {
        File nodePath = new File("node_local");
        File yarnPath = new File("yarn_local");

        FrontendToolsLocator locatorMock = mock(FrontendToolsLocator.class);
        when(locatorMock.tryLocateTool("node"))
                .thenReturn(Optional.of(nodePath));
        when(locatorMock.tryLocateTool("yarn"))
                .thenReturn(Optional.of(yarnPath));

        RunnerManager runnerManager = new RunnerManager.Builder(
                downloadDirectory.getRoot(), proxyConfig).autodetectTools(true)
                        .frontendToolsLocator(locatorMock).build();

        assertNotNull("Frontend tool runners should be present",
                runnerManager.getGulpRunner());
        assertNotNull("Frontend tool runners should be present",
                runnerManager.getYarnRunner());
        assertArrayEquals(
                "No file should be downloaded when local tools are used",
                new String[] {}, downloadDirectory.getRoot().list());
        verify(locatorMock, times(0)).verifyTool(nodePath);
        verify(locatorMock, times(0)).verifyTool(yarnPath);
        verify(locatorMock, times(1)).tryLocateTool("node");
        verify(locatorMock, times(1)).tryLocateTool("yarn");
    }

    @Test
    public void onlyMissingToolsAreSearchedFor_yarn() {
        File nodePath = new File("node_local");
        File yarnPath = new File("yarn_local");

        FrontendToolsLocator locatorMock = mock(FrontendToolsLocator.class);
        when(locatorMock.verifyTool(nodePath)).thenReturn(true);
        when(locatorMock.tryLocateTool("yarn"))
                .thenReturn(Optional.of(yarnPath));

        RunnerManager runnerManager = new RunnerManager.Builder(
                downloadDirectory.getRoot(), proxyConfig)
                        .localInstallations(nodePath, null)
                        .autodetectTools(true).frontendToolsLocator(locatorMock)
                        .build();

        assertNotNull("Frontend tool runners should be present",
                runnerManager.getGulpRunner());
        assertNotNull("Frontend tool runners should be present",
                runnerManager.getYarnRunner());
        assertArrayEquals(
                "No file should be downloaded when local tools are used",
                new String[] {}, downloadDirectory.getRoot().list());
        verify(locatorMock, times(1)).verifyTool(nodePath);
        verify(locatorMock, times(0)).verifyTool(yarnPath);
        verify(locatorMock, times(0)).tryLocateTool("node");
        verify(locatorMock, times(1)).tryLocateTool("yarn");
    }

    @Test
    public void onlyMissingToolsAreSearchedFor_node() {
        File nodePath = new File("node_local");
        File yarnPath = new File("yarn_local");

        FrontendToolsLocator locatorMock = mock(FrontendToolsLocator.class);
        when(locatorMock.verifyTool(yarnPath)).thenReturn(true);
        when(locatorMock.tryLocateTool("node"))
                .thenReturn(Optional.of(nodePath));

        RunnerManager runnerManager = new RunnerManager.Builder(
                downloadDirectory.getRoot(), proxyConfig)
                .localInstallations(null, yarnPath)
                .autodetectTools(true).frontendToolsLocator(locatorMock)
                .build();

        assertNotNull("Frontend tool runners should be present",
                runnerManager.getGulpRunner());
        assertNotNull("Frontend tool runners should be present",
                runnerManager.getYarnRunner());
        assertArrayEquals(
                "No file should be downloaded when local tools are used",
                new String[] {}, downloadDirectory.getRoot().list());
        verify(locatorMock, times(0)).verifyTool(nodePath);
        verify(locatorMock, times(1)).verifyTool(yarnPath);
        verify(locatorMock, times(1)).tryLocateTool("node");
        verify(locatorMock, times(0)).tryLocateTool("yarn");
    }
}
