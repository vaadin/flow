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

import static com.vaadin.flow.server.frontend.FrontendUtils.TAILWIND_JS;

public class TaskGenerateTailwindJsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File npmFolder;
    private File frontendGeneratedFolder;
    private TaskGenerateTailwindJs taskGenerateTailwindJs;

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        frontendGeneratedFolder = new File(npmFolder, "src/frontend-generated");
        frontendGeneratedFolder.mkdirs();
        Options options = new Options(Mockito.mock(Lookup.class), npmFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder);
        taskGenerateTailwindJs = new TaskGenerateTailwindJs(options);
    }

    @Test
    public void should_haveCorrectFileContent() throws Exception {
        verifyTailwindJs(taskGenerateTailwindJs.getFileContent());
    }

    @Test
    public void should_generateTailwindCss() throws Exception {
        File tailwindjs = new File(frontendGeneratedFolder, TAILWIND_JS);
        taskGenerateTailwindJs.execute();
        Assert.assertEquals("Should have correct tailwind.js file path",
                tailwindjs, taskGenerateTailwindJs.getGeneratedFile());
        verifyTailwindJs(getTailwindCssFileContent());
        Assert.assertTrue(
                "Should generate tailwind.js in the frontend generated folder",
                taskGenerateTailwindJs.shouldGenerate());
    }

    @Test
    public void should_updateExistingTailwindCss() throws Exception {
        File tailwindcss = new File(frontendGeneratedFolder, TAILWIND_JS);
        Files.writeString(tailwindcss.toPath(), "OLD CONTENT");
        taskGenerateTailwindJs.execute();
        Assert.assertTrue(
                "Should generate tailwind.css in the frontend generated folder",
                taskGenerateTailwindJs.shouldGenerate());
        var tailwindCssContent = getTailwindCssFileContent();
        Assert.assertEquals("Should update content in tailwind.css",
                taskGenerateTailwindJs.getFileContent(), tailwindCssContent);
    }

    private void verifyTailwindJs(String tailwindJsContent) {
        Assert.assertTrue("Should have tailwind.css import",
                tailwindJsContent.contains(
                        "import tailwindCss from './tailwind.css?inline';\n"));
        Assert.assertTrue("Should define applyTailwindCss function",
                tailwindJsContent.contains("function applyTailwindCss(css)"));
        Assert.assertTrue("Should apply Tailwind CSS",
                tailwindJsContent.contains("applyTailwindCss(tailwindCss);\n"));
        Assert.assertTrue("Should inject as global CSS",
                tailwindJsContent.contains(
                        "injectGlobalCss(css.toString(), 'CSSImport end', document);\n"));
        Assert.assertTrue("Should support hot module reload", tailwindJsContent
                .contains("import.meta.hot.accept('./tailwind.css?inline',"));
    }

    private String getTailwindCssFileContent() throws IOException {
        return Files
                .readString(taskGenerateTailwindJs.getGeneratedFile().toPath());
    }
}
