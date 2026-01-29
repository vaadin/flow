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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.frontend.scanner.ChunkInfo;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.frontend.FrontendUtils.TAILWIND_CSS;
import static com.vaadin.flow.server.frontend.scanner.ChunkInfo.GLOBAL;

public class TaskGenerateTailwindCssTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File npmFolder;
    private File frontendGeneratedFolder;
    private File frontendFolder;
    private TaskGenerateTailwindCss taskGenerateTailwindCss;
    private FrontendDependenciesScanner frontendDependenciesScanner;

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        File srcFolder = new File(npmFolder, "src");
        srcFolder.mkdirs();
        frontendFolder = new File(srcFolder, "frontend");
        frontendFolder.mkdirs();
        frontendGeneratedFolder = new File(frontendFolder, "generated");
        frontendGeneratedFolder.mkdirs();

        frontendDependenciesScanner = Mockito
                .mock(FrontendDependenciesScanner.class);

        Options options = new Options(Mockito.mock(Lookup.class), null,
                npmFolder)
                .withFrontendDependenciesScanner(frontendDependenciesScanner)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder);
        taskGenerateTailwindCss = new TaskGenerateTailwindCss(options);
    }

    @Test
    public void should_haveCorrectFileContent() throws Exception {
        verifyTailwindCss(taskGenerateTailwindCss.getFileContent(), false,
                false);
    }

    @Test
    public void should_generateTailwindCss() throws Exception {
        File tailwindcss = new File(frontendGeneratedFolder, TAILWIND_CSS);
        taskGenerateTailwindCss.execute();
        Assert.assertEquals("Should have correct tailwind.css file path",
                tailwindcss, taskGenerateTailwindCss.getGeneratedFile());
        verifyTailwindCss(getTailwindCssFileContent(), false, false);
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
    public void should_includeThemeImport_whenThemeStylesCssExists()
            throws Exception {
        // Create theme folder with styles.css
        String themeName = "my-theme";
        File themesFolder = new File(frontendFolder, "themes");
        File themeFolder = new File(themesFolder, themeName);
        themeFolder.mkdirs();
        File stylesCss = new File(themeFolder, "styles.css");
        Files.writeString(stylesCss.toPath(),
                ".custom-class { @apply text-blue-500; }");

        ThemeDefinition themeDefinition = Mockito.mock(ThemeDefinition.class);
        Mockito.when(frontendDependenciesScanner.getThemeDefinition())
                .thenReturn(themeDefinition);
        Mockito.when(themeDefinition.getName()).thenReturn(themeName);

        // Recreate task with theme name
        Options options = new Options(Mockito.mock(Lookup.class), null,
                npmFolder)
                .withFrontendDependenciesScanner(frontendDependenciesScanner)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder);
        TaskGenerateTailwindCss task = new TaskGenerateTailwindCss(options);

        String content = task.getFileContent();
        Assert.assertTrue("Should have theme styles.css import",
                content.contains("@import '../themes/my-theme/styles.css';"));
        Assert.assertFalse("Should not contain backslashes in import path",
                content.contains("\\"));
    }

    @Test
    public void should_notIncludeThemeImport_whenNoTheme() throws Exception {
        Options options = new Options(Mockito.mock(Lookup.class), null,
                npmFolder)
                .withFrontendDependenciesScanner(frontendDependenciesScanner)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder);
        TaskGenerateTailwindCss task = new TaskGenerateTailwindCss(options);

        String content = task.getFileContent();
        verifyTailwindCss(content, false, false);
    }

    @Test
    public void should_notIncludeThemeImport_whenThemeStylesCssNotExists()
            throws Exception {
        // Create theme folder without styles.css
        String themeName = "my-theme";
        File themesFolder = new File(frontendFolder, "themes");
        File themeFolder = new File(themesFolder, themeName);
        themeFolder.mkdirs();

        Options options = new Options(Mockito.mock(Lookup.class), null,
                npmFolder)
                .withFrontendDependenciesScanner(frontendDependenciesScanner)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder);
        TaskGenerateTailwindCss task = new TaskGenerateTailwindCss(options);

        String content = task.getFileContent();
        Assert.assertFalse(
                "Should not have theme import when styles.css missing",
                content.contains("themes/my-theme/styles.css"));
    }

    @Test
    public void should_includeThemeImport_whenJarPackagedThemeStylesCssExists()
            throws Exception {
        // Create JAR resources folder with theme styles.css
        String themeName = "jar-theme";
        File jarResourcesFolder = new File(frontendGeneratedFolder,
                "jar-resources");
        File themesFolder = new File(jarResourcesFolder, "themes");
        File themeFolder = new File(themesFolder, themeName);
        themeFolder.mkdirs();
        File stylesCss = new File(themeFolder, "styles.css");
        Files.writeString(stylesCss.toPath(),
                ".jar-class { @apply text-red-500; }");

        ThemeDefinition themeDefinition = Mockito.mock(ThemeDefinition.class);
        Mockito.when(frontendDependenciesScanner.getThemeDefinition())
                .thenReturn(themeDefinition);
        Mockito.when(themeDefinition.getName()).thenReturn(themeName);

        // Recreate task with JAR resources folder
        Options options = new Options(Mockito.mock(Lookup.class), null,
                npmFolder)
                .withFrontendDependenciesScanner(frontendDependenciesScanner)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder)
                .withJarFrontendResourcesFolder(jarResourcesFolder);
        TaskGenerateTailwindCss task = new TaskGenerateTailwindCss(options);

        String content = task.getFileContent();
        Assert.assertTrue("Should have JAR theme styles.css import",
                content.contains(
                        "@import './jar-resources/themes/jar-theme/styles.css';"));
        Assert.assertFalse("Should not contain backslashes in import path",
                content.contains("\\"));
    }

    @Test
    public void should_preferLocalTheme_overJarPackagedTheme()
            throws Exception {
        // Create both local and JAR-packaged theme with same name
        String themeName = "shared-theme";

        // Local theme
        File localThemesFolder = new File(frontendFolder, "themes");
        File localThemeFolder = new File(localThemesFolder, themeName);
        localThemeFolder.mkdirs();
        File localStylesCss = new File(localThemeFolder, "styles.css");
        Files.writeString(localStylesCss.toPath(),
                ".local-class { @apply text-blue-500; }");

        // JAR-packaged theme
        File jarResourcesFolder = new File(frontendGeneratedFolder,
                "jar-resources");
        File jarThemesFolder = new File(jarResourcesFolder, "themes");
        File jarThemeFolder = new File(jarThemesFolder, themeName);
        jarThemeFolder.mkdirs();
        File jarStylesCss = new File(jarThemeFolder, "styles.css");
        Files.writeString(jarStylesCss.toPath(),
                ".jar-class { @apply text-red-500; }");

        ThemeDefinition themeDefinition = Mockito.mock(ThemeDefinition.class);
        Mockito.when(frontendDependenciesScanner.getThemeDefinition())
                .thenReturn(themeDefinition);
        Mockito.when(themeDefinition.getName()).thenReturn(themeName);

        // Recreate task with both folders
        Options options = new Options(Mockito.mock(Lookup.class), null,
                npmFolder)
                .withFrontendDependenciesScanner(frontendDependenciesScanner)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder)
                .withJarFrontendResourcesFolder(jarResourcesFolder);
        TaskGenerateTailwindCss task = new TaskGenerateTailwindCss(options);

        String content = task.getFileContent();
        // Should prefer local theme over JAR theme
        Assert.assertTrue("Should have local theme styles.css import", content
                .contains("@import '../themes/shared-theme/styles.css';"));
        Assert.assertFalse("Should not have JAR theme import", content
                .contains("./jar-resources/themes/shared-theme/styles.css"));
    }

    @Test
    public void should_includeCssImport_whenJarResourceCssExists()
            throws Exception {
        // Create theme folder without styles.css
        File generatedFolder = new File(frontendFolder, "generated");
        File jarResources = new File(generatedFolder, "jar-resources");
        File addOn = new File(jarResources, "add-on");
        addOn.mkdirs();
        File addOnCss = new File(addOn, "add-on.css");
        addOnCss.createNewFile();
        Map<ChunkInfo, List<CssData>> addOns = Collections.singletonMap(GLOBAL,
                List.of(new CssData("./add-on/add-on.css", null, null, null)));
        Mockito.when(frontendDependenciesScanner.getCss()).thenReturn(addOns);

        Options options = new Options(Mockito.mock(Lookup.class), null,
                npmFolder)
                .withFrontendDependenciesScanner(frontendDependenciesScanner)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(frontendGeneratedFolder);
        TaskGenerateTailwindCss task = new TaskGenerateTailwindCss(options);

        String content = task.getFileContent();
        Assert.assertFalse("Should have add-on.css import",
                content.contains("./jar-resources/add-on.css"));
    }

    private void verifyTailwindCss(String tailwindCssContent,
            boolean shouldHaveCustomImport, boolean shouldHaveThemeImport) {
        Assert.assertTrue("Should have tailwindcss/theme.css import",
                tailwindCssContent
                        .contains("@import 'tailwindcss/theme.css';"));
        Assert.assertTrue("Should have tailwindcss/utilities.css import",
                tailwindCssContent
                        .contains("@import 'tailwindcss/utilities.css';"));
        Assert.assertTrue("Should have @source directive with path",
                tailwindCssContent.contains("@source '../..';"));
        if (shouldHaveCustomImport) {
            Assert.assertTrue("Should have custom import", tailwindCssContent
                    .contains("@import '../tailwind-custom.css';"));
        } else {
            Assert.assertFalse("Should not have custom import",
                    tailwindCssContent.contains("tailwind-custom.css"));
        }
        if (shouldHaveThemeImport) {
            Assert.assertTrue("Should have theme import",
                    tailwindCssContent.contains("themes/"));
        } else {
            Assert.assertFalse("Should not have theme import",
                    tailwindCssContent.contains("themes/"));
        }
    }

    private String getTailwindCssFileContent() throws IOException {
        return Files.readString(
                taskGenerateTailwindCss.getGeneratedFile().toPath());
    }
}
