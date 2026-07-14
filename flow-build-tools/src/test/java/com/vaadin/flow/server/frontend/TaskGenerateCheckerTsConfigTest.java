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
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGenerateCheckerTsConfigTest {

    @TempDir
    File temporaryFolder;

    private TaskGenerateCheckerTsConfig task;

    @BeforeEach
    void setUp() throws IOException {
        File npmFolder = Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile();
        Options options = new Options(Mockito.mock(Lookup.class), npmFolder)
                .withBuildDirectory("target");
        task = new TaskGenerateCheckerTsConfig(options);
    }

    @Test
    void generatesCheckerTsConfigExcludingJarResources() throws Exception {
        task.execute();

        File generated = task.getGeneratedFile();
        assertTrue(generated.exists(),
                "checker tsconfig should be generated in the build directory");
        assertTrue(generated.getParentFile().getName().equals("target"),
                "checker tsconfig should live in the build directory");

        String content = Files.readString(generated.toPath());
        assertTrue(content.contains("jar-resources/**"),
                "must keep excluding jar-resources from type checking");
        assertFalse(content.contains("%FRONTEND%"),
                "frontend token must be substituted");
        assertTrue(content.contains("src/main/frontend"),
                "frontend path must be substituted with the project path");
    }

    @Test
    void alwaysOverwrites() {
        assertTrue(task.shouldGenerate(),
                "checker tsconfig is a build artifact and must be regenerated");
    }
}
