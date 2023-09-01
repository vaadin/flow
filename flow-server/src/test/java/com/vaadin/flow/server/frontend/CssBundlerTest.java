package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
                        getThemeFile("styles.css")));

        writeCss("background-image: url(\"foo/bar.png\");", "styles.css");

        Assert.assertEquals(
                "background-image: url('VAADIN/themes/my-theme/foo/bar.png');",
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css")));

        writeCss("background-image: url(foo/bar.png);", "styles.css");

        Assert.assertEquals(
                "background-image: url('VAADIN/themes/my-theme/foo/bar.png');",
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css")));
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
                CssBundler
                        .inlineImports(themeFolder, getThemeFile("styles.css"))
                        .trim());
    }

    @Test
    public void relativeUrlsInSubFolderRewritten() throws IOException {
        writeCss("@import url('sub/sub.css');", "styles.css");
        writeCss("background-image: url('./file.png');", "sub/sub.css");
        createThemeFile("sub/file.png");

        Assert.assertEquals(
                "background-image: url('VAADIN/themes/my-theme/sub/file.png');",
                CssBundler.inlineImports(themeFolder,
                        getThemeFile("styles.css")));
    }

    @Test
    public void dollarAndBackslashWorks() throws IOException {
        String css = "body { content: '$\\'}";
        writeCss("@import 'other.css';", "styles.css");
        writeCss(css, "other.css");

        Assert.assertEquals("body { content: '$\\'}", CssBundler
                .inlineImports(themeFolder, getThemeFile("styles.css")));
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
}
