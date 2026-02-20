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
import com.vaadin.flow.internal.FrontendUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGenerateTailwindJsTest {

    @TempDir
    File temporaryFolder;

    private File npmFolder;
    private File frontendGeneratedFolder;
    private TaskGenerateTailwindJs taskGenerateTailwindJs;

    @BeforeEach
    void setUp() throws IOException {
        npmFolder = Files.createTempDirectory(temporaryFolder.toPath(), "tmp")
                .toFile();
        frontendGeneratedFolder = new File(npmFolder, "src/frontend-generated");
        frontendGeneratedFolder.mkdirs();
        Options options = new Options(Mockito.mock(Lookup.class), npmFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder);
        taskGenerateTailwindJs = new TaskGenerateTailwindJs(options);
    }

    @Test
    void should_haveCorrectFileContent() throws Exception {
        verifyTailwindJs(taskGenerateTailwindJs.getFileContent());
    }

    @Test
    void should_generateTailwindCss() throws Exception {
        File tailwindjs = new File(frontendGeneratedFolder,
                FrontendUtils.TAILWIND_JS);
        taskGenerateTailwindJs.execute();
        assertEquals(tailwindjs, taskGenerateTailwindJs.getGeneratedFile(),
                "Should have correct tailwind.js file path");
        verifyTailwindJs(getTailwindCssFileContent());
        assertTrue(taskGenerateTailwindJs.shouldGenerate(),
                "Should generate tailwind.js in the frontend generated folder");
    }

    @Test
    void should_updateExistingTailwindCss() throws Exception {
        File tailwindcss = new File(frontendGeneratedFolder,
                FrontendUtils.TAILWIND_JS);
        Files.writeString(tailwindcss.toPath(), "OLD CONTENT");
        taskGenerateTailwindJs.execute();
        assertTrue(taskGenerateTailwindJs.shouldGenerate(),
                "Should generate tailwind.css in the frontend generated folder");
        var tailwindCssContent = getTailwindCssFileContent();
        assertEquals(taskGenerateTailwindJs.getFileContent(),
                tailwindCssContent, "Should update content in tailwind.css");
    }

    private void verifyTailwindJs(String tailwindJsContent) {
        assertTrue(
                tailwindJsContent.contains(
                        "import tailwindCss from './tailwind.css?inline';"
                                + System.lineSeparator()),
                "Should have tailwind.css import");
        assertTrue(tailwindJsContent.contains("function applyTailwindCss(css)"),
                "Should define applyTailwindCss function");
        assertTrue(tailwindJsContent.contains(
                "applyTailwindCss(tailwindCss);" + System.lineSeparator()),
                "Should apply Tailwind CSS");
        assertTrue(tailwindJsContent.contains(
                "injectGlobalCss(css.toString(), 'CSSImport end', document);"
                        + System.lineSeparator()),
                "Should inject as global CSS");
        assertTrue(
                tailwindJsContent.contains(
                        "import.meta.hot.accept('./tailwind.css?inline',"),
                "Should support hot module reload");
    }

    private String getTailwindCssFileContent() throws IOException {
        return Files
                .readString(taskGenerateTailwindJs.getGeneratedFile().toPath());
    }
}
