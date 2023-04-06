package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class CssBundlerTest {

    private static final String TEST_CSS = "body {background: blue};";

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
        File dir = Files.createTempDirectory("cssbundlertest").toFile();
        File stylesCss = new File(dir, "styles.css");
        FileUtils.writeStringToFile(stylesCss, css, StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(dir, otherFilename), TEST_CSS,
                StandardCharsets.UTF_8);
        return stylesCss;
    }
}
