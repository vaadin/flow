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
package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PublicStyleSheetBundlerTest {

    private static final String EXPECTED_CSS = """
                DIV.appshell-image {
                    background: url('../images/gobo.png') no-repeat;
                    background-size: contain;
                    background-position-x: center;
                    width: 100%;
                    height: 100px;
                }
            """;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void bundle_inlinesImportedCss_returnsMergedContent()
            throws IOException {
        // Arrange a temporary fake project structure
        File project = temporaryFolder.newFolder("project");
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        File mainCss = new File(publicRoot, "main.css");
        File importedCss = new File(publicRoot, "imported.css");

        // The imported file should be inlined into main
        String importedContent = ".imported{background:blue;}";
        String mainContent = "@import './imported.css';\n.main{color:red;}\n";

        Files.writeString(importedCss.toPath(), importedContent,
                StandardCharsets.UTF_8);
        Files.writeString(mainCss.toPath(), mainContent,
                StandardCharsets.UTF_8);

        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        Mockito.when(config.getJavaResourceFolder())
                .thenReturn(new File(project, "src/main/resources"));

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .create(config);

        // Act
        Optional<String> bundled = bundler.bundle("/main.css");

        // Assert
        assertTrue("Bundled CSS should be present", bundled.isPresent());
        String result = normalizeWhitespace(bundled.get());
        // Should contain both imported and main rules
        assertTrue("Result should contain imported content",
                result.contains(".imported{background:blue;}"));
        assertTrue("Result should contain main content",
                result.contains(".main{color:red;}"));
        // Imported content should appear before main rule when @import is first
        assertTrue("Imported content should precede main content",
                result.indexOf(".imported{background:blue;}") < result
                        .indexOf(".main{color:red;}"));
    }

    @Test
    public void bundle_supportsContextProtocol() throws IOException {
        File project = temporaryFolder.newFolder("project2");
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        Files.writeString(new File(publicRoot, "imported.css").toPath(),
                ".im{b:1;}", StandardCharsets.UTF_8);
        Files.writeString(new File(publicRoot, "main.css").toPath(),
                "@import './imported.css';\n.m{c:2;}", StandardCharsets.UTF_8);

        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        Mockito.when(config.getJavaResourceFolder())
                .thenReturn(new File(project, "src/main/resources"));

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .create(config);

        Optional<String> bundled = bundler.bundle("context://main.css");
        assertTrue(bundled.isPresent());
        String result = normalizeWhitespace(bundled.get());
        assertTrue(result.contains(".im{b:1;}"));
        assertTrue(result.contains(".m{c:2;}"));
        assertTrue(result.indexOf(".im{b:1;}") < result.indexOf(".m{c:2;}"));
    }

    @Test
    public void normalize_contextProtocol_isStripped() {
        assertEquals("css/app.css",
                PublicStyleSheetBundler.normalizeUrl("context://css/app.css"));
    }

    @Test
    public void normalize_leadingSlash_isRemoved() {
        assertEquals("css/app.css",
                PublicStyleSheetBundler.normalizeUrl("/css/app.css"));
    }

    @Test
    public void normalize_relativeDotSlash_isRemoved() {
        assertEquals("css/app.css",
                PublicStyleSheetBundler.normalizeUrl("./css/app.css"));
    }

    @Test
    public void normalize_backslashes_areConverted() {
        assertEquals("/css/app.css",
                PublicStyleSheetBundler.normalizeUrl("\\css\\app.css"));
    }

    @Test
    public void bundle_preservesUrls_inTopLevelCss() throws IOException {
        File project = temporaryFolder.newFolder("project_meta");
        File metaInfRoot = new File(project,
                "src/main/resources/META-INF/resources/css");
        assertTrue(metaInfRoot.mkdirs());
        File styles = new File(metaInfRoot, "styles.css");
        Files.writeString(styles.toPath(), EXPECTED_CSS,
                StandardCharsets.UTF_8);

        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        Mockito.when(config.getJavaResourceFolder())
                .thenReturn(new File(project, "src/main/resources"));

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .create(config);

        Optional<String> bundled = bundler.bundle("/css/styles.css");
        assertTrue("Bundled CSS should be present", bundled.isPresent());
        String result = normalizeWhitespace(bundled.get());
        String expected = normalizeWhitespace(EXPECTED_CSS);
        assertEquals("URL paths in top-level CSS should be preserved", expected,
                result);
        assertFalse("Should not rewrite to VAADIN/themes",
                result.contains("VAADIN/themes"));
    }

    @Test
    public void bundle_withImport_inlinesAndRebasesNestedUrls()
            throws IOException {
        // Arrange META-INF/resources with a nested import that has a relative
        // URL
        File project = temporaryFolder.newFolder("project_meta_import");
        File metaInfRoot = new File(project,
                "src/main/resources/META-INF/resources/css");
        assertTrue(metaInfRoot.mkdirs());
        File nestedDir = new File(metaInfRoot, "nested");
        assertTrue(nestedDir.mkdirs());
        // nested/nested-imported.css contains url('../images/gobo.png')
        Files.writeString(new File(nestedDir, "nested-imported.css").toPath(),
                EXPECTED_CSS, StandardCharsets.UTF_8);
        // styles.css imports the nested file
        Files.writeString(new File(metaInfRoot, "styles.css").toPath(),
                "@import './nested/nested-imported.css';\n/* main marker */",
                StandardCharsets.UTF_8);

        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        Mockito.when(config.getJavaResourceFolder())
                .thenReturn(new File(project, "src/main/resources"));

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .create(config);

        Optional<String> bundled = bundler.bundle("/css/styles.css");
        assertTrue(bundled.isPresent());
        String result = normalizeWhitespace(bundled.get());
        String expectedImported = normalizeWhitespace(
                EXPECTED_CSS.replace("../images/gobo.png", "images/gobo.png"));
        assertTrue(
                "Imported nested content should be inlined with rebased URLs",
                result.contains(expectedImported));
        assertTrue("Main marker should be present after imported content",
                result.contains(expectedImported));
        assertFalse("Should not rewrite to VAADIN/themes",
                result.contains("VAADIN/themes"));
    }

    private static String normalizeWhitespace(String s) {
        assertNotNull(s);
        return s.replaceAll("\\s+", " ").trim();
    }
}
