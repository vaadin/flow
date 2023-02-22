package com.vaadin.base.devserver;

import com.vaadin.flow.testutil.TestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class ThemeModifierTest {

    private String FRONTEND_FOLDER = "themeeditor/META-INF/frontend";

    private String FRONTEND_NO_THEME_FOLDER = "themeeditor-empty/META-INF/frontend";

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
        System.getProperties().put(ThemeModifier.THEME_EDITOR_ENABLED_PROPERTY,
                "true");
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

    @After
    public void cleanup() {
        System.getProperties()
                .remove(ThemeModifier.THEME_EDITOR_ENABLED_PROPERTY);
        File themeFolder = new File(
                TestUtils.getTestFolder(FRONTEND_NO_THEME_FOLDER), "themes");
        if (themeFolder.exists()) {
            new File(themeFolder, "my-theme/styles.css").delete();
            new File(themeFolder, "my-theme/theme-editor.css").delete();
            new File(themeFolder, "my-theme").delete();
            themeFolder.delete();
        }
    }

    @Test
    public void themeFolderPresent_stateEnabled() {
        ThemeModifier themeModifier = new TestThemeModifier();
        Assert.assertEquals(ThemeModifier.State.ENABLED,
                themeModifier.getState());
    }

    @Test
    public void themeDoesNotExists_stateMissingTheme() {
        ThemeModifier themeModifier = new TestThemeModifier() {
            @Override
            protected File getFrontendFolder() {
                return TestUtils.getTestFolder(FRONTEND_NO_THEME_FOLDER);
            }
        };
        Assert.assertEquals(ThemeModifier.State.MISSING_THEME,
                themeModifier.getState());
    }

    @Test
    public void themeDoesNotExists_createDefaultTheme_themeIsCreated() {
        ThemeModifier themeModifier = new TestThemeModifier() {
            @Override
            protected File getFrontendFolder() {
                return TestUtils.getTestFolder(FRONTEND_NO_THEME_FOLDER);
            }
        };
        Assert.assertEquals(ThemeModifier.State.MISSING_THEME,
                themeModifier.getState());

        themeModifier.createDefaultTheme();
        themeModifier = new TestThemeModifier() {
            @Override
            protected File getFrontendFolder() {
                return TestUtils.getTestFolder(FRONTEND_NO_THEME_FOLDER);
            }
        };
        Assert.assertEquals(ThemeModifier.State.ENABLED,
                themeModifier.getState());
    }

    @Test
    public void themeEditorPropertyNotSet_stateDisabled() {
        System.getProperties()
                .remove(ThemeModifier.THEME_EDITOR_ENABLED_PROPERTY);
        ThemeModifier themeModifier = new TestThemeModifier();
        Assert.assertEquals(ThemeModifier.State.DISABLED,
                themeModifier.getState());
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
    public void rulesWithSameSelectorExists_rulesAreGrouped() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssRule(SELECTOR_WITH_PART, "font-family", "serif");
        assertThemeEditorCssContains(SELECTOR_WITH_PART + " {");
        assertThemeEditorCssContains("color:red;");
        assertThemeEditorCssContains("font-family:serif;");
    }

    @Test
    public void rulesAreAddedRandomly_rulesAreGroupedAndSorted() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule("vaadin-button::part(label)", "color", "red");
        modifier.setCssRule("vaadin-button", "font-family", "serif");
        modifier.setCssRule("vaadin-button", "color", "brown");
        modifier.setCssRule("vaadin-button::part(label)", "border",
                "1px solid red");
        List<String> lines = getFileLines("theme-editor.css");
        Assert.assertEquals("vaadin-button {", lines.get(4));
        Assert.assertEquals("  color:brown;", lines.get(5));
        Assert.assertEquals("  font-family:serif;", lines.get(6));
        Assert.assertEquals("}", lines.get(7));
        Assert.assertEquals("vaadin-button::part(label) {", lines.get(9));
        Assert.assertEquals("  border:1px solid red;", lines.get(10));
        Assert.assertEquals("  color:red;", lines.get(11));
        Assert.assertEquals("}", lines.get(12));
    }

    @Test
    public void rulesWithSameSelectorExists_ruleIsUpdated() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssRule(SELECTOR_WITH_PART, "font-family", "serif");
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "blue");
        assertThemeEditorCssNotContains("color:red;");
        assertThemeEditorCssContains("color:blue;");
    }

    @Test
    public void ruleIsRemoved_rulesUpdated() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssRule(SELECTOR_WITH_PART, "font-family", "serif");
        modifier.removeCssRule(SELECTOR_WITH_PART, "color");
        assertThemeEditorCssNotContains("color:red;");
    }

    @Test
    public void allRulesAreRemoved_ruleIsNotPresent() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssRule(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssRule(SELECTOR_WITH_PART, "font-family", "serif");
        modifier.removeCssRule(SELECTOR_WITH_PART, "color");
        modifier.removeCssRule(SELECTOR_WITH_PART, "font-family");
        assertThemeEditorCssNotContains(SELECTOR_WITH_PART);
    }

    private void assertThemeEditorCssNotContains(String string) {
        Assert.assertTrue(getFileLines("theme-editor.css").stream()
                .noneMatch(line -> line.contains(string)));
    }

    private void assertThemeEditorCssContains(String string) {
        Assert.assertTrue(getFileLines("theme-editor.css").stream()
                .anyMatch(line -> line.contains(string)));
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
