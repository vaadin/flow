package com.vaadin.base.devserver.themeeditor;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import com.vaadin.base.devserver.themeeditor.utils.CssRule;
import com.vaadin.flow.testutil.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ThemeModifierTest extends AbstractThemeEditorTest {

    @Before
    public void prepareFiles() throws IOException {
        super.prepare();
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
    public void noImport_propertyAdded_importPresent() {
        CascadingStyleSheet styleSheet = getStylesheet("styles.css");
        assertTrue(styleSheet.getAllImportRules().isEmpty());

        ThemeModifier modifier = new TestThemeModifier();
        modifier.setThemeProperties(Collections.singletonList(
                new CssRule(SELECTOR_WITH_PART, "color", "red")));

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
        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "color", "red"));
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "font-family", "serif"));
        modifier.setThemeProperties(toBeAdded);

        styleSheet = getStylesheet("styles.css");
        assertEquals(1, styleSheet.getImportRuleCount());
        assertEquals("theme-editor.css",
                styleSheet.getImportRuleAtIndex(0).getLocationString());
    }

    @Test
    public void ruleAdded_ruleIsPresent() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setThemeProperties(Collections.singletonList(
                new CssRule(SELECTOR_WITH_PART, "color", "red")));

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());

        CSSStyleRule expected = modifier.createStyleRule(SELECTOR_WITH_PART,
                "color", "red");
        assertTrue(styleSheet.getAllStyleRules().contains(expected));
    }

    @Test
    public void locateLines() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.setThemeProperties(Collections.singletonList(
                new CssRule(SELECTOR_WITH_PART, "color", "red")));

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());

        CSSStyleRule expected = modifier.createStyleRule(SELECTOR_WITH_PART,
                "color", "red");
        assertTrue(styleSheet.getAllStyleRules().contains(expected));

        int line = modifier.getRuleLocationLine(SELECTOR_WITH_PART);
        assertEquals(5, line); // 4-line comment before

        line = modifier.getRuleLocationLine("vaadin-app-layout");
        assertEquals(-1, line); // not found
    }

    @Test
    public void emptyRuleAdded_ruleIsPresent() {
        ThemeModifier modifier = new TestThemeModifier();
        modifier.createEmptyStyleRule("vaadin-button");
        List<CssRule> rules = modifier.getCssRules(List.of("vaadin-button"));
        assertEquals(1, rules.size());
        assertTrue(rules.get(0).getProperties().isEmpty());
    }

    @Test
    public void ruleExists_ruleIsUpdated() {
        ThemeModifier modifier = new TestThemeModifier();
        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "color", "red"));
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "color", "blue"));
        modifier.setThemeProperties(toBeAdded);

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
        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "color", "red"));
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "font-family", "serif"));
        modifier.setThemeProperties(toBeAdded);

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());

        CSSStyleRule expected = modifier.createStyleRule(SELECTOR_WITH_PART,
                "color", "");
        CSSStyleRule found = modifier.findRuleBySelector(styleSheet, expected);
        assertNotNull(found);

        assertEquals(2, found.getDeclarationCount());
        assertEquals("red", found.getDeclarationOfPropertyName("color")
                .getExpressionAsCSSString());
        assertEquals("serif", found.getDeclarationOfPropertyName("font-family")
                .getExpressionAsCSSString());
    }

    @Test
    public void rulesAreAddedRandomly_rulesAreGroupedAndSorted() {
        ThemeModifier modifier = new TestThemeModifier();
        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded
                .add(new CssRule("vaadin-button::part(label)", "color", "red"));
        toBeAdded.add(new CssRule("vaadin-button", "font-family", "serif"));
        toBeAdded.add(new CssRule("vaadin-button", "color", "brown"));
        toBeAdded.add(new CssRule("vaadin-button::part(label)", "border",
                "1px solid red"));
        modifier.setThemeProperties(toBeAdded);

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
        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "color", "red"));
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "font-family", "serif"));
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "color", "blue"));
        modifier.setThemeProperties(toBeAdded);

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
        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "color", "red"));
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "font-family", "serif"));
        modifier.setThemeProperties(toBeAdded);

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());
        assertEquals(2,
                styleSheet.getStyleRuleAtIndex(0).getDeclarationCount());

        modifier.setThemeProperties(Collections
                .singletonList(new CssRule(SELECTOR_WITH_PART, "color", "")));

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
        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "color", "red"));
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "font-family", "serif"));
        modifier.setThemeProperties(toBeAdded);

        CascadingStyleSheet styleSheet = getStylesheet("theme-editor.css");
        assertEquals(1, styleSheet.getStyleRuleCount());
        assertEquals(2,
                styleSheet.getStyleRuleAtIndex(0).getDeclarationCount());

        List<CssRule> toBeRemoved = new ArrayList<>();
        toBeRemoved.add(new CssRule(SELECTOR_WITH_PART, "font-family", null));
        toBeRemoved.add(new CssRule(SELECTOR_WITH_PART, "color", null));
        modifier.setThemeProperties(toBeRemoved);

        styleSheet = getStylesheet("theme-editor.css");
        assertEquals(0, styleSheet.getStyleRuleCount());
    }

    @Test
    public void getCss() {
        ThemeModifier modifier = new TestThemeModifier();
        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "color", "red"));
        toBeAdded.add(new CssRule(SELECTOR_WITH_PART, "font-family", "serif"));
        modifier.setThemeProperties(toBeAdded);

        String css = modifier.getCss();
        String expected = "vaadin-text-field::part(label){color:red;font-family:serif}";

        assertEquals(expected, css);
    }

    @Test
    public void getCssRules() {
        ThemeModifier modifier = new TestThemeModifier();
        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded.add(new CssRule("vaadin-button", "color", "red"));
        toBeAdded.add(new CssRule("vaadin-button", "background", "black"));
        toBeAdded.add(new CssRule("vaadin-button::part(label)", "font-family",
                "serif"));
        toBeAdded.add(new CssRule("vaadin-text-field", "color", "red"));
        toBeAdded.add(new CssRule("span", "color", "red"));
        modifier.setThemeProperties(toBeAdded);

        List<CssRule> cssRules = modifier.getCssRules(
                Arrays.asList("vaadin-button", "vaadin-button::part(label)"));
        assertEquals(2, cssRules.size());

        assertEquals("vaadin-button", cssRules.get(0).getSelector());
        assertEquals(2, cssRules.get(0).getProperties().size());
        assertTrue(cssRules.get(0).getProperties().containsKey("color"));
        assertEquals("red", cssRules.get(0).getProperties().get("color"));
        assertTrue(cssRules.get(0).getProperties().containsKey("background"));
        assertEquals("black",
                cssRules.get(0).getProperties().get("background"));

        assertEquals("vaadin-button::part(label)",
                cssRules.get(1).getSelector());
        assertEquals(1, cssRules.get(1).getProperties().size());
        assertTrue(cssRules.get(1).getProperties().containsKey("font-family"));
        assertEquals("serif",
                cssRules.get(1).getProperties().get("font-family"));
    }

    @Test
    public void testClassNameReplace() {
        ThemeModifier modifier = new TestThemeModifier();

        List<CssRule> toBeAdded = new ArrayList<>();
        toBeAdded.add(new CssRule("vaadin-button.nice", "color", "red"));
        toBeAdded.add(
                new CssRule("vaadin-button.not-nice", "background", "black"));
        toBeAdded.add(new CssRule("vaadin-button.nice::part(label)",
                "font-family", "serif"));
        toBeAdded.add(
                new CssRule("vaadin-text-field.perfect.nice", "color", "red"));
        toBeAdded
                .add(new CssRule("span.nice.nice.nice.strong", "color", "red"));
        modifier.setThemeProperties(toBeAdded);

        modifier.replaceClassName("vaadin-button", "nice", "nicer");
        List<CssRule> rules = modifier.getCssRules(Arrays.asList(
                "vaadin-button.nicer", "vaadin-button.nicer::part(label)",
                "vaadin-button.nicer", "vaadin-button.nicer::part(label)",
                "vaadin-text-field.perfect.nicer",
                "span.nicer.nicer.nicer.strong"));
        assertEquals(2, rules.size());

        modifier.replaceClassName("span", "nice", "nicer");
        rules = modifier.getCssRules(Arrays.asList("vaadin-button.nicer",
                "vaadin-button.nicer::part(label)", "vaadin-button.nicer",
                "vaadin-button.nicer::part(label)",
                "vaadin-text-field.perfect.nicer",
                "span.nicer.nicer.nicer.strong"));
        assertEquals(3, rules.size());
    }

    private File getThemeFile(String fileName) {
        File themeFolder = TestUtils
                .getTestFolder(FRONTEND_FOLDER + "/themes/my-theme");
        return new File(themeFolder, fileName);
    }

    private CascadingStyleSheet getStylesheet(String fileName) {
        File themeEditorCss = getThemeFile(fileName);
        return CSSReader.readFromFile(themeEditorCss, StandardCharsets.UTF_8,
                ECSSVersion.LATEST);
    }

}
