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
package com.vaadin.quarkus.deployment.vaadinplugin;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.deployment.CodeGenContext;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The collector exists because a Quarkus build only knows the workspace during
 * code generation, so what matters is when it decides to run and that what it
 * writes can be read back by the build steps that need it.
 */
class WorkspaceInfoCollectorTest {

    @TempDir
    Path workDir;

    @TempDir
    Path projectDir;

    private WorkspaceInfoCollector collector;
    private Config config;

    @BeforeEach
    void setUp() {
        collector = new WorkspaceInfoCollector();
        config = mock(Config.class);
        configure("vaadin.build.enabled", null);
        configure("quarkus.bootstrap.workspace-discovery", null);
    }

    private void configure(String key, Boolean value) {
        when(config.getOptionalValue(key, Boolean.class))
                .thenReturn(Optional.ofNullable(value));
    }

    private void initWithModule() {
        ApplicationModel model = mock(ApplicationModel.class);
        when(model.getApplicationModule()).thenReturn(WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("com.example", "demo", "1.0"))
                .setModuleDir(projectDir)
                .setBuildDir(projectDir.resolve("target")).build());
        collector.init(model, Map.of());
    }

    @Test
    void providerIdAndInputDirectory_identifyTheProvider() {
        assertEquals("vaadin-plugin-workspace-info", collector.providerId());
        // No sources to generate from: this provider only observes the
        // workspace, so it reads nothing.
        assertEquals("", collector.inputDirectory());
    }

    @Test
    void shouldRun_moduleAvailableAndNothingDisabled_runs() {
        initWithModule();

        assertEquals(true, collector.shouldRun(projectDir, config));
    }

    @Test
    void shouldRun_neverInitialized_doesNotRun() {
        assertFalse(collector.shouldRun(projectDir, config),
                "without a module there is nothing to collect");
    }

    @Test
    void shouldRun_vaadinBuildDisabled_doesNotRun() {
        initWithModule();
        configure("vaadin.build.enabled", false);

        assertFalse(collector.shouldRun(projectDir, config),
                "nothing consumes the information when the embedded plugin is "
                        + "switched off");
    }

    @Test
    void shouldRun_workspaceDiscoveryEnabled_doesNotRun() {
        initWithModule();
        configure("quarkus.bootstrap.workspace-discovery", true);

        assertFalse(collector.shouldRun(projectDir, config),
                "the build steps already have the workspace, so collecting it "
                        + "here would be pointless work");
    }

    @Test
    void trigger_moduleAvailable_writesWhatLoadReadsBack() throws Exception {
        initWithModule();

        assertFalse(collector.trigger(context()),
                "the provider generates no sources, so it reports no output");

        WorkspaceModule loaded = WorkspaceInfo.load(workDir);
        assertNotNull(loaded);
        assertEquals("demo", loaded.getId().getArtifactId());
    }

    @Test
    void trigger_neverInitialized_writesNothing() throws Exception {
        assertFalse(collector.trigger(context()));

        assertNull(WorkspaceInfo.load(workDir),
                "nothing should have been written without a module");
    }

    @Test
    void trigger_workDirectoryMissing_reportsACodeGenException() {
        initWithModule();
        CodeGenContext context = mock(CodeGenContext.class);
        when(context.workDir()).thenReturn(workDir.resolve("does-not-exist"));

        CodeGenException exception = assertThrows(CodeGenException.class,
                () -> collector.trigger(context));
        assertNotNull(exception.getCause(),
                "the underlying failure has to survive the wrapping");
    }

    private CodeGenContext context() {
        CodeGenContext context = mock(CodeGenContext.class);
        when(context.workDir()).thenReturn(workDir);
        return context;
    }
}
