package com.vaadin.base.devserver;

import com.vaadin.flow.testutil.TestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class ThemeModifierTest {

    private String FRONTEND_FOLDER = "themeeditor/META-INF/frontend";

    private String SELECTOR_WITH_PART = "vaadin-text-field::part(label)";

    private class TestThemeModifier extends ThemeModifier {

        public TestThemeModifier() {
            super(new MockVaadinContext());
        }

        @Override
        protected File getFrontendFolder() {
            return TestUtils.getTestFolder(FRONTEND_FOLDER);
        }
    }

    @Before
    public void prepareFiles() throws IOException {
        File themeFolder = TestUtils
                .getTestFolder(FRONTEND_FOLDER + "/themes/my-theme");
        File stylesCss = new File(themeFolder, "styles.css");
        if (stylesCss.exists()) {
            stylesCss.delete();
        }
        stylesCss.createNewFile();
        File themeEditorCss = new File(themeFolder, "theme-editor.css");
        if (themeEditorCss.exists()) {
            themeEditorCss.delete();
        }
    }

    @Test
    public void noImport_importPresent() {
        String string = "@import \"theme-editor.css\";";
        Assert.assertTrue(
                getFileLines("styles.css").stream().noneMatch(string::equals));

        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "red");

        Assert.assertTrue(
                getFileLines("styles.css").stream().anyMatch(string::equals));
    }

    @Test
    public void multipleRulesAdded_singleImportPresent() {
        String string = "@import \"theme-editor.css\";";
        Assert.assertTrue(
                getFileLines("styles.css").stream().noneMatch(string::equals));

        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssRule(SELECTOR_WITH_PART, "font-family", "serif");

        Assert.assertTrue(getFileLines("styles.css").stream()
                .filter(string::equals).count() == 1);
    }

    @Test
    public void ruleAdded_ruleIsPresent() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "red");
        assertThemeEditorCssContains(SELECTOR_WITH_PART + " { color:red; }");
    }

    @Test
    public void ruleExists_ruleIsUpdated() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "blue");
        assertThemeEditorCssNotContains(SELECTOR_WITH_PART + " { color:red; }");
        assertThemeEditorCssContains(SELECTOR_WITH_PART + " { color:blue; }");
    }

    @Test
    public void rulesWithSameSelectorExists_ruleIsUpdated() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssRule(SELECTOR_WITH_PART, "font-family", "serif");
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "blue");
        assertThemeEditorCssNotContains(SELECTOR_WITH_PART + " { color:red; }");
        assertThemeEditorCssContains(SELECTOR_WITH_PART + " { color:blue; }");
        assertThemeEditorCssContains(
                SELECTOR_WITH_PART + " { font-family:serif; }");
    }

    private void assertThemeEditorCssNotContains(String string) {
        Assert.assertTrue(getFileLines("theme-editor.css").stream()
                .noneMatch(string::equals));
    }

    private void assertThemeEditorCssContains(String string) {
        Assert.assertTrue(getFileLines("theme-editor.css").stream()
                .anyMatch(string::equals));
    }

    private List<String> getFileLines(String file) {
        File themeFolder = TestUtils
                .getTestFolder(FRONTEND_FOLDER + "/themes/my-theme");
        File themeEditorCss = new File(themeFolder, file);
        try {
            return IOUtils.readLines(new FileReader(themeEditorCss));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
