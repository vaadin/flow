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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ResolvedDependency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.internal.StringUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuarkusPluginAdapterTest {

    // A real filesystem root, so the fixtures below are absolute on Windows
    // too, where a path like /project has no drive letter and is therefore
    // relative. The directories need not exist, buildFolder() never touches
    // the filesystem.
    private static final Path ROOT = Paths.get("").toAbsolutePath().getRoot();

    private VaadinBuildTimeConfig config;
    private ApplicationModel model;
    private WorkspaceModule appModule;
    private final List<ResolvedDependency> runtimeDependencies = new ArrayList<>();

    @BeforeEach
    void setUp() {
        config = mock(VaadinBuildTimeConfig.class);
        appModule = mock(WorkspaceModule.class);
        when(appModule.getModuleDir())
                .thenReturn(ROOT.resolve("project").toFile());
        when(appModule.getBuildDir()).thenReturn(
                ROOT.resolve(Paths.get("project", "target")).toFile());
        when(appModule.hasMainSources()).thenReturn(false);

        model = mock(ApplicationModel.class);
        when(model.getRuntimeDependencies()).thenReturn(runtimeDependencies);
    }

    private QuarkusPluginAdapter createAdapter() {
        return new QuarkusPluginAdapter(config, model, appModule);
    }

    private void addRuntimeDependency(String groupId, String artifactId) {
        ResolvedDependency dependency = mock(ResolvedDependency.class);
        when(dependency.getGroupId()).thenReturn(groupId);
        when(dependency.getArtifactId()).thenReturn(artifactId);
        runtimeDependencies.add(dependency);
    }

    @Test
    void checkRuntimeDependency_dependencyPresent_noMessage() {
        addRuntimeDependency("com.vaadin", "license-checker");
        List<String> messages = new ArrayList<>();

        assertTrue(createAdapter().checkRuntimeDependency("com.vaadin",
                "license-checker", messages::add));
        assertTrue(messages.isEmpty(),
                "No message expected for a present dependency");
    }

    @Test
    void checkRuntimeDependency_onlyGroupIdMatches_reportsMissing() {
        // com.vaadin is always a runtime dependency of a Vaadin application,
        // so matching on the group id alone would never report the artifact as
        // missing.
        addRuntimeDependency("com.vaadin", "vaadin-quarkus");
        List<String> messages = new ArrayList<>();

        assertFalse(createAdapter().checkRuntimeDependency("com.vaadin",
                "license-checker", messages::add));
        assertEquals(1, messages.size(),
                "Missing dependency message expected: " + messages);
        assertTrue(
                messages.get(0).contains("com.vaadin:license-checker")
                        && messages.get(0).contains("<scope>runtime</scope>"),
                "Unexpected message: " + messages.get(0));
    }

    @Test
    void checkRuntimeDependency_noDependencies_reportsMissing() {
        List<String> messages = new ArrayList<>();

        assertFalse(createAdapter().checkRuntimeDependency("com.vaadin",
                "license-checker", messages::add));
        assertEquals(1, messages.size());
    }

    @Test
    void checkRuntimeDependency_nullMessageConsumer_doesNotFail() {
        assertFalse(createAdapter().checkRuntimeDependency("com.vaadin",
                "license-checker", null));
    }

    @Test
    void checkRuntimeDependency_nullGroupIdOrArtifactId_throws() {
        QuarkusPluginAdapter adapter = createAdapter();
        assertThrows(NullPointerException.class, () -> adapter
                .checkRuntimeDependency(null, "license-checker", null));
        assertThrows(NullPointerException.class,
                () -> adapter.checkRuntimeDependency("com.vaadin", null, null));
    }

    @Test
    void applicationIdentifier_notConfigured_hashesGroupIdColonArtifactId() {
        when(config.applicationIdentifier()).thenReturn(Optional.empty());
        ResolvedDependency appArtifact = mock(ResolvedDependency.class);
        when(appArtifact.getGroupId()).thenReturn("com.example");
        when(appArtifact.getArtifactId()).thenReturn("my-app");
        when(model.getAppArtifact()).thenReturn(appArtifact);

        assertEquals(
                "app-" + StringUtil.getHash("com.example:my-app",
                        StandardCharsets.UTF_8),
                createAdapter().applicationIdentifier());
    }

    @Test
    void applicationIdentifier_configured_returnsConfiguredValue() {
        when(config.applicationIdentifier())
                .thenReturn(Optional.of("my-identifier"));

        assertEquals("my-identifier", createAdapter().applicationIdentifier());
    }

    @Test
    void buildFolder_insideProjectDirectory_relativeToProject() {
        when(appModule.getBuildDir()).thenReturn(
                ROOT.resolve(Paths.get("project", "target")).toFile());

        assertEquals("target", createAdapter().buildFolder());
    }

    @Test
    void buildFolder_outsideProjectDirectory_parentRelativePath() {
        when(appModule.getBuildDir()).thenReturn(
                ROOT.resolve(Paths.get("builds", "target")).toFile());

        // Must be relative, since consumers resolve it against the project
        // folder with new File(npmFolder(), buildFolder())
        assertEquals(
                ".." + File.separator + "builds" + File.separator + "target",
                createAdapter().buildFolder());
    }

    @Test
    void buildFolder_relativeBuildDirectory_returnedUnchanged() {
        when(appModule.getBuildDir()).thenReturn(new File("build/classes"));

        assertEquals("build" + File.separator + "classes",
                createAdapter().buildFolder());
    }

    @Test
    void minimumFrontendPackageAgeDays_configured_returnsValue() {
        when(config.minimumFrontendPackageAgeDays()).thenReturn(Optional.of(7));

        assertEquals(7, createAdapter().minimumFrontendPackageAgeDays());
    }

    @Test
    void minimumFrontendPackageAgeDays_notConfigured_returnsNull() {
        when(config.minimumFrontendPackageAgeDays())
                .thenReturn(Optional.empty());

        // null lets the frontend tools keep their own minimum release age
        assertNull(createAdapter().minimumFrontendPackageAgeDays());
    }
}
