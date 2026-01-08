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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;

import static com.vaadin.flow.internal.FrontendUtils.TAILWIND_CSS;

public class TaskGenerateTailwindCssTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File npmFolder;
    private File frontendGeneratedFolder;
    private File frontendFolder;
    private TaskGenerateTailwindCss taskGenerateTailwindCss;

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        File srcFolder = new File(npmFolder, "src");
        srcFolder.mkdirs();
        frontendFolder = new File(srcFolder, "frontend");
        frontendFolder.mkdirs();
        frontendGeneratedFolder = new File(frontendFolder, "generated");
        frontendGeneratedFolder.mkdirs();
        Options options = new Options(Mockito.mock(Lookup.class), npmFolder)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder);
        taskGenerateTailwindCss = new TaskGenerateTailwindCss(options);
    }

    @Test
    public void should_haveCorrectFileContent() throws Exception {
        verifyTailwindCss(taskGenerateTailwindCss.getFileContent(), false);
    }

    @Test
    public void should_generateTailwindCss() throws Exception {
        File tailwindcss = new File(frontendGeneratedFolder, TAILWIND_CSS);
        taskGenerateTailwindCss.execute();
        Assert.assertEquals("Should have correct tailwind.css file path",
                tailwindcss, taskGenerateTailwindCss.getGeneratedFile());
        verifyTailwindCss(getTailwindCssFileContent(), false);
        Assert.assertTrue(
                "Should generate tailwind.css in the frontend generated folder",
                taskGenerateTailwindCss.shouldGenerate());
    }

    @Test
    public void should_updateExistingTailwindCss() throws Exception {
        File tailwindcss = new File(frontendGeneratedFolder, TAILWIND_CSS);
        Files.writeString(tailwindcss.toPath(), "OLD CONTENT");
        taskGenerateTailwindCss.execute();
        Assert.assertTrue(
                "Should generate tailwind.css in the frontend generated folder",
                taskGenerateTailwindCss.shouldGenerate());
        var tailwindCssContent = getTailwindCssFileContent();
        Assert.assertEquals("Should update content in tailwind.css",
                taskGenerateTailwindCss.getFileContent(), tailwindCssContent);
    }

    @Test
    public void should_includeCustomImport_whenCustomFileExists()
            throws Exception {
        // Create custom CSS file in the src/frontend folder (parent of
        // generated folder)
        File customCss = new File(frontendFolder, "tailwind-custom.css");
        Files.writeString(customCss.toPath(),
                "@theme { --color-my-theme: red; }");

        // Recreate task to pick up the custom file
        Options options = new Options(Mockito.mock(Lookup.class), npmFolder)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder);
        TaskGenerateTailwindCss task = new TaskGenerateTailwindCss(options);

        String content = task.getFileContent();
        verifyTailwindCss(content, true);
        Assert.assertFalse("Should not contain backslashes in import path",
                content.contains("\\"));
    }

    private void verifyTailwindCss(String tailwindCssContent,
            boolean shouldHaveCustomImport) {
        Assert.assertTrue("Should have tailwindcss/theme.css import",
                tailwindCssContent
                        .contains("@import 'tailwindcss/theme.css';\n"));
        Assert.assertTrue("Should have tailwindcss/utilities.css import",
                tailwindCssContent
                        .contains("@import 'tailwindcss/utilities.css';\n"));
        Assert.assertTrue("Should have @source directive with path",
                tailwindCssContent.contains("@source '../..';\n"));
        if (shouldHaveCustomImport) {
            Assert.assertTrue("Should have custom import", tailwindCssContent
                    .contains("@import '../tailwind-custom.css';"));
        } else {
            Assert.assertFalse("Should not have custom import",
                    tailwindCssContent.contains("tailwind-custom.css"));
        }
    }

    private String getTailwindCssFileContent() throws IOException {
        return Files.readString(
                taskGenerateTailwindCss.getGeneratedFile().toPath());
    }
}
