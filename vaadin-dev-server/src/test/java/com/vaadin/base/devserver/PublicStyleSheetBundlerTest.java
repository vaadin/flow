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
package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicStyleSheetBundlerTest {

    private static final String EXPECTED_CSS = """
                DIV.appshell-image {
                    background: url('../images/gobo.png') no-repeat;
                    background-size: contain;
                    background-position-x: center;
                    width: 100%;
                    height: 100px;
                }
            """;

    @TempDir
    File temporaryFolder;

    @Test
    void bundle_inlinesImportedCss_returnsMergedContent() throws IOException {
        // Arrange a temporary fake project structure
        File project = new File(temporaryFolder, "project");
        project.mkdirs();
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

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .forResourceLocations(java.util.List.of(publicRoot));

        // Act
        Optional<String> bundled = bundler.bundle("/main.css", "");

        // Assert
        assertTrue(bundled.isPresent(), "Bundled CSS should be present");
        String result = normalizeWhitespace(bundled.get());
        // Should contain both imported and main rules
        assertTrue(result.contains(".imported{background:blue;}"),
                "Result should contain imported content");
        assertTrue(result.contains(".main{color:red;}"),
                "Result should contain main content");
        // Imported content should appear before main rule when @import is first
        assertTrue(
                result.indexOf(".imported{background:blue;}") < result
                        .indexOf(".main{color:red;}"),
                "Imported content should precede main content");
    }

    @Test
    void bundle_supportsContextProtocol() throws IOException {
        File project = new File(temporaryFolder, "project2");
        project.mkdirs();
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        Files.writeString(new File(publicRoot, "imported.css").toPath(),
                ".im{b:1;}", StandardCharsets.UTF_8);
        Files.writeString(new File(publicRoot, "main.css").toPath(),
                "@import './imported.css';\n.m{c:2;}", StandardCharsets.UTF_8);

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .forResourceLocations(java.util.List.of(publicRoot));

        Optional<String> bundled = bundler.bundle("context://main.css", "");
        assertTrue(bundled.isPresent());
        String result = normalizeWhitespace(bundled.get());
        assertTrue(result.contains(".im{b:1;}"));
        assertTrue(result.contains(".m{c:2;}"));
        assertTrue(result.indexOf(".im{b:1;}") < result.indexOf(".m{c:2;}"));
    }

    @Test
    void normalize_contextProtocol_isStripped() {
        assertEquals("css/app.css",
                PublicStyleSheetBundler.normalizeUrl("context://css/app.css"));
    }

    @Test
    void normalize_leadingSlash_isRemoved() {
        assertEquals("css/app.css",
                PublicStyleSheetBundler.normalizeUrl("/css/app.css"));
    }

    @Test
    void normalize_relativeDotSlash_isRemoved() {
        assertEquals("css/app.css",
                PublicStyleSheetBundler.normalizeUrl("./css/app.css"));
    }

    @Test
    void normalize_backslashes_areConverted() {
        assertEquals("/css/app.css",
                PublicStyleSheetBundler.normalizeUrl("\\css\\app.css"));
    }

    @Test
    void bundle_preservesUrls_inTopLevelCss() throws IOException {
        String given = """
                DIV.appshell-image {
                    background: url('./gobo.png') no-repeat;
                }
                """;
        File project = new File(temporaryFolder, "project_meta");
        project.mkdirs();
        File publicRoot = new File(project,
                "src/main/resources/META-INF/resources");
        assertTrue(publicRoot.mkdirs());
        File image = new File(publicRoot, "gobo.png");
        Files.writeString(image.toPath(), "", StandardCharsets.UTF_8);
        File styles = new File(publicRoot, "styles.css");
        Files.writeString(styles.toPath(), given, StandardCharsets.UTF_8);

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .forResourceLocations(java.util.List.of(publicRoot));

        Optional<String> bundled = bundler.bundle("./styles.css", "");
        assertTrue(bundled.isPresent(), "Bundled CSS should be present");
        String result = normalizeWhitespace(bundled.get());
        String expected = normalizeWhitespace(
                given.replace("./gobo.png", "/gobo.png"));
        assertEquals(expected, result,
                "URL paths in top-level CSS should be preserved");
    }

    @Test
    void bundle_withImport_inlinesAndRebasesNestedUrls() throws IOException {
        // @formatter:off
        // src/main/resources
        // └── META-INF
        //     └── resources
        //         └── css
        //             ├── images
        //             │   └── gobo.png
        //             ├── nested
        //             │   └── nested-imported.css
        //             └── styles.css
        // @formatter:on
        File project = new File(temporaryFolder, "project_meta_import");
        project.mkdirs();
        File publicRoot = new File(project,
                "src/main/resources/META-INF/resources");
        File cssRoot = new File(publicRoot, "css");
        assertTrue(cssRoot.mkdirs());
        File nestedDir = new File(cssRoot, "nested");
        assertTrue(nestedDir.mkdirs());
        File imagesDir = new File(cssRoot, "images");
        assertTrue(imagesDir.mkdirs());
        Files.writeString(new File(imagesDir, "gobo.png").toPath(), "",
                StandardCharsets.UTF_8);
        // nested/nested-imported.css contains url('../images/gobo.png')
        Files.writeString(new File(nestedDir, "nested-imported.css").toPath(),
                EXPECTED_CSS, StandardCharsets.UTF_8);
        // styles.css imports the nested file
        Files.writeString(new File(cssRoot, "styles.css").toPath(),
                "@import './nested/nested-imported.css';\n/* main marker */",
                StandardCharsets.UTF_8);

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .forResourceLocations(java.util.List.of(publicRoot));

        Optional<String> bundled = bundler.bundle("/css/styles.css", "");
        assertTrue(bundled.isPresent());
        String result = normalizeWhitespace(bundled.get());
        String expectedImported = normalizeWhitespace(EXPECTED_CSS
                .replace("../images/gobo.png", "/css/images/gobo.png"));
        assertTrue(result.contains(expectedImported),
                "Imported nested content should be inlined with rebased URLs");
        assertTrue(result.contains(expectedImported),
                "Main marker should be present after imported content");
        assertFalse(result.contains("VAADIN/themes"),
                "Should not rewrite to VAADIN/themes");
    }

    @Test
    void bundle_withImport_inlinesAndRebasesDoubleNestedUrls()
            throws IOException {
        // @formatter:off
        // src/main/resources
        // └── META-INF
        //     └── resources
        //         └── css
        //             ├── images
        //             │   └── viking.png
        //             └── view
        //                 ├── imported.css
        //                 ├── nested
        //                 │ └── nested-imported.css
        //                 └── view.css
        // @formatter:on
        File project = new File(temporaryFolder, "project_meta_import");
        project.mkdirs();
        File publicRoot = new File(project,
                "src/main/resources/META-INF/resources");
        File cssRoot = new File(publicRoot, "css");
        assertTrue(cssRoot.mkdirs());
        File viewDir = new File(cssRoot, "view");
        assertTrue(viewDir.mkdirs());
        File nestedDir = new File(viewDir, "nested");
        assertTrue(nestedDir.mkdirs());
        File imagesDir = new File(cssRoot, "images");
        assertTrue(imagesDir.mkdirs());
        Files.writeString(new File(imagesDir, "viking.png").toPath(), "",
                StandardCharsets.UTF_8);
        // nested/nested-imported.css contains background-image:
        // url(../../images/viking.png);
        String given = """
                DIV.nested-imported {
                    background-image: url('../../images/viking.png');
                }
                """;
        Files.writeString(new File(nestedDir, "nested-imported.css").toPath(),
                given, StandardCharsets.UTF_8);
        // imported.css imports the nested file
        Files.writeString(new File(viewDir, "imported.css").toPath(),
                "@import 'nested/nested-imported.css';",
                StandardCharsets.UTF_8);
        // view.css imports the sibling file
        Files.writeString(new File(viewDir, "view.css").toPath(),
                "@import 'imported.css';", StandardCharsets.UTF_8);

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .forResourceLocations(java.util.List.of(publicRoot));

        Optional<String> bundled = bundler.bundle("/css/view/view.css", "");
        assertTrue(bundled.isPresent());
        String result = normalizeWhitespace(bundled.get());
        String expected = normalizeWhitespace(given
                .replace("../../images/viking.png", "/css/images/viking.png"));
        assertEquals(expected, result,
                "Unexpected bundled content for double nested sub-directories");
    }

    @Test
    void bundle_withImport_inlinesWithContextPath() throws IOException {
        File project = new File(temporaryFolder, "project_inline_ctx");
        project.mkdirs();
        File publicRoot = new File(project,
                "src/main/resources/META-INF/resources");
        File cssRoot = new File(publicRoot, "css");
        assertTrue(cssRoot.mkdirs());
        File viewDir = new File(cssRoot, "view");
        assertTrue(viewDir.mkdirs());
        File nestedDir = new File(viewDir, "nested");
        assertTrue(nestedDir.mkdirs());
        File imagesDir = new File(cssRoot, "images");
        assertTrue(imagesDir.mkdirs());
        Files.writeString(new File(imagesDir, "viking.png").toPath(), "",
                StandardCharsets.UTF_8);
        String nested = """
                DIV.nested-imported {
                    background-image: url('../../images/viking.png');
                }
                """;
        Files.writeString(new File(nestedDir, "nested-imported.css").toPath(),
                nested, StandardCharsets.UTF_8);
        Files.writeString(new File(viewDir, "imported.css").toPath(),
                "@import 'nested/nested-imported.css';",
                StandardCharsets.UTF_8);
        Files.writeString(new File(viewDir, "view.css").toPath(),
                "@import 'imported.css';", StandardCharsets.UTF_8);

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .forResourceLocations(java.util.List.of(publicRoot));

        Optional<String> bundled = bundler.bundle("/css/view/view.css",
                "/context");
        assertTrue(bundled.isPresent());
        String result = normalizeWhitespace(bundled.get());
        String expected = normalizeWhitespace(nested.replace(
                "../../images/viking.png", "/context/css/images/viking.png"));
        assertEquals(expected, result,
                "Inline bundled content should contain absolute context-aware URL");
    }

    @Test
    void bundleInline_topLevel_rewritesToAbsoluteWithContextPath()
            throws IOException {
        String given = """
                DIV.appshell-image {
                    background: url('./gobo.png') no-repeat;
                }
                """;
        File project = new File(temporaryFolder, "project_inline_top_level");
        project.mkdirs();
        File publicRoot = new File(project,
                "src/main/resources/META-INF/resources");
        assertTrue(publicRoot.mkdirs());
        File cssDir = new File(publicRoot, "css");
        assertTrue(cssDir.mkdirs());
        Files.writeString(new File(cssDir, "gobo.png").toPath(), "",
                StandardCharsets.UTF_8);
        File styles = new File(cssDir, "styles.css");
        Files.writeString(styles.toPath(), given, StandardCharsets.UTF_8);

        PublicStyleSheetBundler bundler = PublicStyleSheetBundler
                .forResourceLocations(java.util.List.of(publicRoot));

        Optional<String> bundled = bundler.bundle("/css/styles.css",
                "/context");
        assertTrue(bundled.isPresent(), "Bundled CSS should be present");
        String result = normalizeWhitespace(bundled.get());
        String expected = normalizeWhitespace(
                given.replace("./gobo.png", "/context/css/gobo.png"));
        assertEquals(expected, result,
                "Top-level URL should be rewritten to absolute with context path");
    }

    private static String normalizeWhitespace(String s) {
        assertNotNull(s);
        return s.replaceAll("\\s+", " ").trim();
    }
}
