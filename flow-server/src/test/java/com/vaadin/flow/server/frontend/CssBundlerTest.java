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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.vaadin.flow.internal.JacksonUtils;

public class CssBundlerTest {

    private static final String TEST_CSS = "body {background: blue};";
    private File themesFolder;
    private File themeFolder;

    @Before
    public void setup() throws IOException {
        themesFolder = Files.createTempDirectory("cssbundlertest").toFile();
        themeFolder = new File(themesFolder, "my-theme");
    }

    @Test
    public void differentImportSyntaxesSupported() throws Exception {
        String[] validImports = new String[] { //
                // The typical you actually use
                "@import url(foo.css);", //
                "@import url(foo.css?ts=1234);", //
                "@import url('foo.css');", //
                "@import url(\"foo.css\");", //
                "@import 'foo.css';", //
                "@import \"foo.css\";", //

                // Invalid according to spec, works in browser
                "@import url(foo.css);", //

                // Extra whitespace
                "  @import    url  (  '  foo.css  '  )  ;   ", //
                "   @import   '  foo.css   '   ;", //
        };

        for (String valid : validImports) {
            assertImportWorks(valid);
        }
    }

    @Test
    public void layerImportsNotHandled() throws IOException {
        assertImportNotHandled("@import url('foo.css') layer(foo);");
        assertImportNotHandled("@import url('foo.css') layer(foo) ;");
        assertImportNotHandled("@import 'theme.css' layer(utilities);");
        assertImportNotHandled("@import \"theme.css\" layer();");
        assertImportNotHandled("@import \"style.css\" layer;");
        assertImportNotHandled("@import \"style.css\" layer print;");
    }

    @Test
    public void conditionalImportsNotHandled() throws IOException {
        assertImportNotHandled("@import url('foo.css') print;");
        assertImportNotHandled("@import url('bluish.css') print, screen;");
        assertImportNotHandled("@import \"common.css\" screen;");
        assertImportNotHandled(
                "@import url('landscape.css') screen and (orientation: landscape);");
    }

    @Test
    public void relativeUrlsRewritten() throws IOException {
        writeCss("background-image: url('foo/bar.png');", "styles.css");
        createThemeFile("foo/bar.png");

        Assert.assertEquals(
                "background-image: url('VAADIN/themes/my-theme/foo/bar.png');",
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css"), getThemeJson()));

        writeCss("background-image: url(\"foo/bar.png\");", "styles.css");

        Assert.assertEquals(
                "background-image: url('VAADIN/themes/my-theme/foo/bar.png');",
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css"), getThemeJson()));

        writeCss("background-image: url(foo/bar.png);", "styles.css");

        Assert.assertEquals(
                "background-image: url('VAADIN/themes/my-theme/foo/bar.png');",
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css"), getThemeJson()));
    }

    @Test
    public void relativeUrlsWithExtraInfoRewritten() throws IOException {
        writeCss(
                """
                        @font-face {
                            font-family: "Ostrich";
                            src: url("./fonts/ostrich-sans-regular.ttf") format("TrueType");
                        }
                        """,
                "styles.css");
        createThemeFile("fonts/ostrich-sans-regular.ttf");

        Assert.assertEquals(
                """
                                                   @font-face {
                            font-family: "Ostrich";
                            src: url('VAADIN/themes/my-theme/fonts/ostrich-sans-regular.ttf') format("TrueType");
                        }"""
                        .trim(),
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css"), getThemeJson()).trim());
    }

    @Test
    public void relativeUrlsInSubFolderRewritten() throws IOException {
        writeCss("@import url('sub/sub.css');", "styles.css");
        writeCss("background-image: url('./file.png');", "sub/sub.css");
        createThemeFile("sub/file.png");

        Assert.assertEquals(
                "background-image: url('VAADIN/themes/my-theme/sub/file.png');",
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css"), getThemeJson()));
    }

    @Test
    public void dollarAndBackslashWorks() throws IOException {
        String css = "body { content: '$\\'}";
        writeCss("@import 'other.css';", "styles.css");
        writeCss(css, "other.css");

        Assert.assertEquals("body { content: '$\\'}", CssBundler.inlineImports(
                themeFolder, getThemeFile("styles.css"), getThemeJson()));
    }

    @Test
    public void unhandledImportsAreMovedToTop() throws IOException {
        writeCss("body {background: blue};", "other.css");
        writeCss(
                """
                        @import url('https://cdn.jsdelivr.net/fontsource/css/inter@latest/index.css');
                        @import url('other.css');
                        @import url('https://cdn.jsdelivr.net/fontsource/css/aclonica@latest/index.css');
                        @import url('https://cdn.jsdelivr.net/fontsource/css/aclonica@latest/index.css?ts=1234');
                        @import url('https://cdn.jsdelivr.net/fontsource/css/aclonica@latest/index.css#foo');
                        @import url('foo.css') layer(foo);
                        @import url('bluish.css') print, screen;
                        @import url('landscape.css') screen and (orientation: landscape);
                        """,
                "styles.css");

        Assert.assertEquals(
                """
                        @import url('https://cdn.jsdelivr.net/fontsource/css/inter@latest/index.css');
                        @import url('https://cdn.jsdelivr.net/fontsource/css/aclonica@latest/index.css');
                        @import url('https://cdn.jsdelivr.net/fontsource/css/aclonica@latest/index.css?ts=1234');
                        @import url('https://cdn.jsdelivr.net/fontsource/css/aclonica@latest/index.css#foo');
                        @import url('foo.css') layer(foo);
                        @import url('bluish.css') print, screen;
                        @import url('landscape.css') screen and (orientation: landscape);

                        body {background: blue};
                        """
                        .trim(),
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css"), getThemeJson()).trim());
    }

    @Test
    public void themeAssetsRelativeUrlsRewritten() throws IOException {
        createThemeJson("""
                {
                  "assets": {
                    "@some/pkg": {
                      "svgs/regular/**": "my/icons"
                    }
                  }
                }
                """);
        writeCss("""
                background-image: url('my/icons/file1.png');
                background-image: url('./my/icons/file2.png');
                background-image: url('../my/icons/file3.png');
                """, "styles.css");

        Assert.assertEquals(
                """
                        background-image: url('VAADIN/themes/my-theme/my/icons/file1.png');
                        background-image: url('VAADIN/themes/my-theme/my/icons/file2.png');
                        background-image: url('../my/icons/file3.png');
                        """,
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css"), getThemeJson()));
    }

    @Test
    public void themeAssetsRelativeUrlsInSubFolderRewritten()
            throws IOException {
        createThemeJson("""
                {
                  "assets": {
                    "@some/pkg": {
                      "svgs/regular/**": "my/icons"
                    }
                  }
                }
                """);
        writeCss("""
                @import url('sub/sub.css');
                @import url('sub/nested/two.css');
                """, "styles.css");
        writeCss("""
                @import url('nested/one.css');
                background-image: url('../my/icons/file.png');
                """, "sub/sub.css");
        writeCss("background-image: url('../../my/icons/file1.png');",
                "sub/nested/one.css");
        writeCss("background-image: url('../../my/icons/file2.png');",
                "sub/nested/two.css");

        String actualCss = CssBundler.inlineImports(themeFolder,
                getThemeFile("styles.css"), getThemeJson());
        Assert.assertEquals(
                """
                        background-image: url('VAADIN/themes/my-theme/my/icons/file1.png');
                        background-image: url('VAADIN/themes/my-theme/my/icons/file.png');

                        background-image: url('VAADIN/themes/my-theme/my/icons/file2.png');
                        """,
                actualCss);
    }

    @Test
    public void relativeUrl_notThemeResourceNotAssets_notRewritten()
            throws IOException {
        createThemeJson("""
                {
                  "assets": {
                    "@some/pkg": {
                      "svgs/regular/**": "my/icons"
                    }
                  }
                }
                """);
        writeCss("""
                @import url('sub/sub.css');
                @import url('sub/nested/two.css');
                background-image: url('unknown/icons/file-root.png');
                """, "styles.css");
        writeCss("""
                @import url('nested/one.css');
                background-image: url('../unknown/icons/file-sub.png');
                """, "sub/sub.css");
        writeCss("background-image: url('../../unknown/icons/file1.png');",
                "sub/nested/one.css");
        writeCss("background-image: url('../../unknown/icons/file2.png');",
                "sub/nested/two.css");

        String actualCss = CssBundler.inlineImports(themeFolder,
                getThemeFile("styles.css"), getThemeJson());
        Assert.assertEquals("""
                background-image: url('../../unknown/icons/file1.png');
                background-image: url('../unknown/icons/file-sub.png');

                background-image: url('../../unknown/icons/file2.png');
                background-image: url('unknown/icons/file-root.png');
                """, actualCss);
    }

    private boolean createThemeFile(String filename) throws IOException {
        File f = getThemeFile(filename);
        f.getParentFile().mkdirs();
        return f.createNewFile();
    }

    private File getThemeFile(String filename) {
        return new File(themeFolder, filename);
    }

    private void assertImportWorks(String importCss) throws IOException {
        File f = writeFileWithImport(importCss, "foo.css");
        Assert.assertEquals(importCss, TEST_CSS.trim(),
                CssBundler.inlineImports(f.getParentFile(), f,
                        new ObjectMapper().createArrayNode()).trim());

    }

    private void assertImportNotHandled(String importCss) throws IOException {
        File f = writeFileWithImport(importCss, "foo.css");
        Assert.assertEquals(importCss, CssBundler.inlineImports(
                f.getParentFile(), f, new ObjectMapper().createArrayNode()));

    }

    private File writeFileWithImport(String css, String otherFilename)
            throws IOException {
        writeCss(TEST_CSS, otherFilename);
        return writeCss(css, "styles.css");
    }

    private File writeCss(String css, String filename) throws IOException {
        File file = getThemeFile(filename);
        FileUtils.writeStringToFile(file, css, StandardCharsets.UTF_8);
        return file;
    }

    private JsonNode getThemeJson() throws IOException {
        File file = getThemeFile("theme.json");
        if (file.exists()) {
            return JacksonUtils.readTree(Files.readString(file.toPath()));
        }
        return null;
    }

    private void createThemeJson(String json) throws IOException {
        JsonNode jsonObject = JacksonUtils.readTree(json);
        File file = getThemeFile("theme.json");
        FileUtils.writeStringToFile(file, jsonObject.toString(),
                StandardCharsets.UTF_8);
    }

    @Test
    public void minifyCss_removesComments() {
        String css = "/* comment */ .class { color: red; }";
        String result = CssBundler.minifyCss(css);
        Assert.assertEquals(".class{color:red}", result);
    }

    @Test
    public void minifyCss_removesMultilineComments() {
        String css = """
                /* This is a
                   multiline comment */
                .class { color: red; }
                """;
        String result = CssBundler.minifyCss(css);
        Assert.assertEquals(".class{color:red}", result);
    }

    @Test
    public void minifyCss_collapsesWhitespace() {
        String css = ".class   {   color:   red;   }";
        String result = CssBundler.minifyCss(css);
        Assert.assertEquals(".class{color:red}", result);
    }

    @Test
    public void minifyCss_removesTrailingSemicolons() {
        String css = ".class { color: red; }";
        String result = CssBundler.minifyCss(css);
        Assert.assertEquals(".class{color:red}", result);
    }

    @Test
    public void minifyCss_handlesMultipleRules() {
        String css = """
                .class1 { color: red; }
                .class2 { background: blue; }
                """;
        String result = CssBundler.minifyCss(css);
        Assert.assertEquals(".class1{color:red}.class2{background:blue}",
                result);
    }

    @Test
    public void minifyCss_preservesSelectorsWithCombinators() {
        String css = ".parent > .child { color: red; }";
        String result = CssBundler.minifyCss(css);
        Assert.assertEquals(".parent>.child{color:red}", result);
    }

    @Test
    public void minifyCss_handlesEmptyInput() {
        Assert.assertEquals("", CssBundler.minifyCss(""));
        Assert.assertEquals("", CssBundler.minifyCss("   "));
        Assert.assertEquals("", CssBundler.minifyCss("/* only comment */"));
    }

    @Test
    public void inlineImports_resolvesNodeModulesImport() throws IOException {
        // Create a node_modules structure
        File nodeModules = new File(themesFolder, "node_modules");
        File packageDir = new File(nodeModules, "some-package");
        packageDir.mkdirs();

        // Create CSS in node_modules
        File nodeModulesCss = new File(packageDir, "styles.css");
        FileUtils.writeStringToFile(nodeModulesCss,
                ".from-node-modules { color: blue; }", StandardCharsets.UTF_8);

        // Create main CSS that imports from node_modules
        writeCss("@import 'some-package/styles.css';\n.main { color: red; }",
                "styles.css");

        String result = CssBundler.inlineImports(themeFolder,
                getThemeFile("styles.css"), null, nodeModules);

        Assert.assertTrue("Should start with node_modules CSS inlined",
                result.startsWith(".from-node-modules { color: blue; }"));
        Assert.assertTrue("Should contain main CSS",
                result.contains(".main { color: red; }"));
    }

    @Test
    public void inlineImports_prefersRelativeOverNodeModules()
            throws IOException {
        // Create a node_modules structure
        File nodeModules = new File(themesFolder, "node_modules");
        File packageDir = new File(nodeModules, "local");
        packageDir.mkdirs();

        // Create CSS in node_modules
        File nodeModulesCss = new File(packageDir, "styles.css");
        FileUtils.writeStringToFile(nodeModulesCss,
                ".from-node-modules { color: blue; }", StandardCharsets.UTF_8);

        // Create local CSS with same relative path
        File localDir = new File(themeFolder, "local");
        localDir.mkdirs();
        File localCss = new File(localDir, "styles.css");
        FileUtils.writeStringToFile(localCss, ".from-local { color: green; }",
                StandardCharsets.UTF_8);

        // Create main CSS that imports using relative path
        writeCss("@import 'local/styles.css';\n.main { color: red; }",
                "styles.css");

        String result = CssBundler.inlineImports(themeFolder,
                getThemeFile("styles.css"), null, nodeModules);

        // Should prefer relative path over node_modules
        Assert.assertTrue("Should start with local CSS inlined",
                result.startsWith(".from-local { color: green; }"));
        Assert.assertFalse("Should not contain node_modules CSS",
                result.contains(".from-node-modules"));
    }

    @Test
    public void inlineImports_handlesNullNodeModulesFolder()
            throws IOException {
        writeCss("@import 'nonexistent/styles.css';\n.main { color: red; }",
                "styles.css");

        // Should not throw when nodeModulesFolder is null
        String result = CssBundler.inlineImports(themeFolder,
                getThemeFile("styles.css"), null, null);

        // Unresolved import should be preserved at top
        Assert.assertTrue("Should preserve unresolved import",
                result.contains("@import 'nonexistent/styles.css'"));
        Assert.assertTrue("Should contain main CSS",
                result.contains(".main { color: red; }"));
    }
}
