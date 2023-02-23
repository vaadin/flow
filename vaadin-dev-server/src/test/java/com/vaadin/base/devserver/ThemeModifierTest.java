package com.vaadin.base.devserver;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import com.vaadin.flow.testutil.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.*;

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
        assertEquals(ThemeModifier.State.ENABLED, themeModifier.getState());
    }

    @Test
    public void themeDoesNotExists_stateMissingTheme() {
        ThemeModifier themeModifier = new TestThemeModifier() {
            @Override
            protected File getFrontendFolder() {
                return TestUtils.getTestFolder(FRONTEND_NO_THEME_FOLDER);
            }
        };
        assertEquals(ThemeModifier.State.MISSING_THEME,
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
        assertEquals(ThemeModifier.State.MISSING_THEME,
                themeModifier.getState());

        themeModifier.createDefaultTheme();
        themeModifier = new TestThemeModifier() {
            @Override
            protected File getFrontendFolder() {
                return TestUtils.getTestFolder(FRONTEND_NO_THEME_FOLDER);
            }
        };
        assertEquals(ThemeModifier.State.ENABLED, themeModifier.getState());
    }

    @Test
    public void themeEditorPropertyNotSet_stateDisabled() {
        System.getProperties()
                .remove(ThemeModifier.THEME_EDITOR_ENABLED_PROPERTY);
        ThemeModifier themeModifier = new TestThemeModifier();
        assertEquals(ThemeModifier.State.DISABLED, themeModifier.getState());
    }

    @Test
    public void noImport_propertyAdded_importPresent() {
        CascadingStyleSheet styleSheet = getStylesheet("styles.css");
        assertTrue(styleSheet.getAllImportRules().isEmpty());

        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "red");

        styleSheet = getStylesheet("styles.css");
        assertEquals(1, styleSheet.getAllImportRules().size());
        assertEquals("theme-editor.css",
                styleSheet.getImportRuleAtIndex(0).getLocationString());
    }

    @Test
    public void multipleRulesAdded_singleImportPresent() {
        CascadingStyleSheet styleSheet = getStylesheet("styles.css");
        assertTrue(styleSheet.getAllImportRules().isEmpty());

        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssProperty(SELECTOR_WITH_PART, "font-family", "serif");

        styleSheet = getStylesheet("styles.css");
        assertEquals(1, styleSheet.getImportRuleCount());
        assertEquals("theme-editor.css",
                styleSheet.getImportRuleAtIndex(0).getLocationString());
    }

    @Test
    public void ruleAdded_ruleIsPresent() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "red");

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());

        CSSStyleRule expected = modifier.createStyleRule(SELECTOR_WITH_PART,
                "color", "red");
        assertTrue(styleSheet.getAllStyleRules().contains(expected));
    }

    @Test
    public void ruleExists_ruleIsUpdated() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "blue");

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());

        CSSStyleRule notExpected = modifier.createStyleRule(SELECTOR_WITH_PART,
                "color", "red");
        assertFalse(styleSheet.getAllStyleRules().contains(notExpected));

        CSSStyleRule expected = modifier.createStyleRule(SELECTOR_WITH_PART,
                "color", "blue");
        assertTrue(styleSheet.getAllStyleRules().contains(expected));
    }

    @Test
    public void rulesWithSameSelectorExists_rulesAreGrouped() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssProperty(SELECTOR_WITH_PART, "font-family", "serif");

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());

        CSSStyleRule expected = modifier.createStyleRule(SELECTOR_WITH_PART,
                "color", "");
        Optional<CSSStyleRule> foundOpt = modifier
                .findRuleBySelector(styleSheet, expected);
        assertTrue(foundOpt.isPresent());

        CSSStyleRule found = foundOpt.get();
        assertEquals(2, found.getDeclarationCount());
        assertEquals("red", found.getDeclarationOfPropertyName("color")
                .getExpressionAsCSSString());
        assertEquals("serif", found.getDeclarationOfPropertyName("font-family")
                .getExpressionAsCSSString());
    }

    @Test
    public void rulesAreAddedRandomly_rulesAreGroupedAndSorted() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssProperty("vaadin-button::part(label)", "color", "red");
        modifier.setCssProperty("vaadin-button", "font-family", "serif");
        modifier.setCssProperty("vaadin-button", "color", "brown");
        modifier.setCssProperty("vaadin-button::part(label)", "border",
                "1px solid red");

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(2, styleSheet.getStyleRuleCount());

        CSSStyleRule expectedFirst = styleSheet.getStyleRuleAtIndex(0);
        assertEquals("vaadin-button",
                expectedFirst.getSelectorAtIndex(0).getAsCSSString());
        assertEquals("color:brown",
                expectedFirst.getDeclarationAtIndex(0).getAsCSSString());
        assertEquals("font-family:serif",
                expectedFirst.getDeclarationAtIndex(1).getAsCSSString());

        CSSStyleRule expectedSecond = styleSheet.getStyleRuleAtIndex(1);
        assertEquals("vaadin-button::part(label)",
                expectedSecond.getSelectorAtIndex(0).getAsCSSString());
        assertEquals("border:1px solid red",
                expectedSecond.getDeclarationAtIndex(0).getAsCSSString());
        assertEquals("color:red",
                expectedSecond.getDeclarationAtIndex(1).getAsCSSString());
    }

    @Test
    public void rulesWithSameSelectorExists_ruleIsUpdated() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssProperty(SELECTOR_WITH_PART, "font-family", "serif");
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "blue");

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());

        CSSStyleRule found = styleSheet.getStyleRuleAtIndex(0);
        assertEquals(2, found.getDeclarationCount());
        assertEquals("blue", found.getDeclarationOfPropertyName("color")
                .getExpressionAsCSSString());
    }

    @Test
    public void ruleIsRemoved_rulesUpdated() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssProperty(SELECTOR_WITH_PART, "font-family", "serif");

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());
        assertEquals(2,
                styleSheet.getStyleRuleAtIndex(0).getDeclarationCount());

        modifier.removeCssProperty(SELECTOR_WITH_PART, "color");

        styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());
        assertEquals(1,
                styleSheet.getStyleRuleAtIndex(0).getDeclarationCount());
        assertNull(styleSheet.getStyleRuleAtIndex(0)
                .getDeclarationOfPropertyName("color"));
    }

    @Test
    public void allRulesAreRemoved_ruleIsNotPresent() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setCssProperty(SELECTOR_WITH_PART, "color", "red");
        modifier.setCssProperty(SELECTOR_WITH_PART, "font-family", "serif");

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());
        assertEquals(2,
                styleSheet.getStyleRuleAtIndex(0).getDeclarationCount());

        modifier.removeCssProperty(SELECTOR_WITH_PART, "color");
        modifier.removeCssProperty(SELECTOR_WITH_PART, "font-family");

        styleSheet = getStylesheet("theme-editor.css");
        assertEquals(0, styleSheet.getStyleRuleCount());
    }

    private CascadingStyleSheet getStylesheet(String fileName) {
        File themeFolder = TestUtils
                .getTestFolder(FRONTEND_FOLDER + "/themes/my-theme");
        File themeEditorCss = new File(themeFolder, fileName);
        return CSSReader.readFromFile(themeEditorCss, StandardCharsets.UTF_8,
                ECSSVersion.LATEST);
    }

}
