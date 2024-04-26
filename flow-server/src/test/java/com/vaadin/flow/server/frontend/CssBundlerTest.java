package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonObject;

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
                CssBundler.inlineImports(f.getParentFile(), f).trim());

    }

    private void assertImportNotHandled(String importCss) throws IOException {
        File f = writeFileWithImport(importCss, "foo.css");
        Assert.assertEquals(importCss,
                CssBundler.inlineImports(f.getParentFile(), f));

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

    private JsonObject getThemeJson() throws IOException {
        File file = getThemeFile("theme.json");
        if (file.exists()) {
            return Json.parse(Files.readString(file.toPath()));
        }
        return null;
    }

    private void createThemeJson(String json) throws IOException {
        JsonObject jsonObject = Json.parse(json);
        File file = getThemeFile("theme.json");
        FileUtils.writeStringToFile(file, jsonObject.toJson(),
                StandardCharsets.UTF_8);
    }
}
