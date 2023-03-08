package com.vaadin.base.devserver.themeeditor;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSImportRule;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.vaadin.base.devserver.themeeditor.utils.CssRule;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ThemeModifier {

    public enum State {
        ENABLED, DISABLED, MISSING_THEME
    }

    private static final String THEME_EDITOR_CSS = "theme-editor.css";

    private static final String HEADER_TEXT = "This file has been created by the Vaadin Theme Editor. Please note that\n"
            + "manual changes to individual CSS properties may be overwritten by the theme editor.";

    private final State state;

    private VaadinContext context;

    private boolean importPresent;

    public ThemeModifier(VaadinContext context) {
        this.context = context;
        this.state = init();
    }

    public boolean isEnabled() {
        return !State.DISABLED.equals(state);
    }

    public State getState() {
        return state;
    }

    /**
     * Performs update of CSS file setting (adding or updating) given
     * {@link CssRule}.
     *
     * @param rules
     *            list of {@link CssRule} to be added or updated
     */
    public void setThemeProperties(List<CssRule> rules) {
        assert rules != null;
        CascadingStyleSheet styleSheet = getCascadingStyleSheet();
        for (CssRule rule : rules) {
            for (Map.Entry<String, String> property : rule.getProperties()
                    .entrySet()) {
                if (property.getValue() != null
                        && !property.getValue().isBlank()) {
                    setCssProperty(styleSheet, rule.getSelector(),
                            property.getKey(), property.getValue());
                } else {
                    removeCssProperty(styleSheet, rule.getSelector(),
                            property.getKey());
                }
            }
        }
        sortStylesheet(styleSheet);
        writeStylesheet(styleSheet);
    }

    /**
     * Returns the full content of the theme editor CSS file.
     *
     * @return CSS string
     */
    public String getCss() {
        File styles = getStyleSheetFile();
        try {
            return Files.readString(Path.of(styles.getAbsolutePath()));
        } catch (IOException e) {
            throw new ThemeEditorException("Could not read stylesheet from "
                    + styles.getAbsolutePath());
        }
    }

    /**
     * Retrieves list of {@link CssRule} for given selector filter.
     *
     * @param selectorFilter
     *            selector filter
     * @return list of {@link CssRule}
     */
    public List<CssRule> getCssRules(Predicate<String> selectorFilter) {
        CascadingStyleSheet styleSheet = getCascadingStyleSheet();
        return styleSheet.getAllStyleRules().stream()
                .filter(rule -> rule.getSelectorCount() > 0)
                .filter(rule -> selectorFilter
                        .test(rule.getSelectorAtIndex(0).getAsCSSString()))
                .map(rule -> {
                    String selector = rule.getSelectorAtIndex(0)
                            .getAsCSSString();
                    Map<String, String> properties = new HashMap<>();
                    rule.getAllDeclarations().forEach(cssDeclaration -> {
                        properties.put(cssDeclaration.getProperty(),
                                cssDeclaration.getExpressionAsCSSString());
                    });
                    return new CssRule(selector, properties);
                }).toList();
    }

    protected String getCssFileName() {
        return THEME_EDITOR_CSS;
    }

    protected String getHeaderText() {
        return HEADER_TEXT;
    }

    protected State init() {
        // for development purposes only
        if (!FeatureFlags.get(context).isEnabled(FeatureFlags.THEME_EDITOR)) {
            return State.DISABLED;
        }

        try {
            getStyleSheetFile();
        } catch (Exception ex) {
            return State.MISSING_THEME;
        }
        return State.ENABLED;
    }

    protected File getFrontendFolder() {
        return new File(ApplicationConfiguration.get(context).getStringProperty(
                FrontendUtils.PROJECT_BASEDIR, null), "frontend");
    }

    protected File getStyleSheetFile() {
        File themes = new File(getFrontendFolder(), "themes");
        String themeName = getThemeName(themes);
        File theme = new File(themes, themeName);
        File themeEditorStyles = new File(theme, getCssFileName());

        if (!themeEditorStyles.exists()) {
            try {
                if (!themeEditorStyles.createNewFile()) {
                    throw new ThemeEditorException(
                            "Cannot create " + themeEditorStyles.getPath());
                }
            } catch (IOException e) {
                throw new ThemeEditorException(
                        "Cannot create " + themeEditorStyles.getPath(), e);
            }
        }

        if (!themeEditorStyles.canWrite()) {
            throw new ThemeEditorException(
                    themeEditorStyles.getPath() + " is not writable.");
        }

        return themeEditorStyles;
    }

    protected CascadingStyleSheet getCascadingStyleSheet() {
        File styles = getStyleSheetFile();
        CascadingStyleSheet styleSheet = CSSReader.readFromFile(styles,
                StandardCharsets.UTF_8, ECSSVersion.LATEST);

        if (!importPresent) {
            insertImportIfNotExists();
            importPresent = true;
        }

        return styleSheet;
    }

    protected void setCssProperty(CascadingStyleSheet styleSheet,
            String selector, String property, String newValue) {
        CSSStyleRule newRule = createStyleRule(selector, property, newValue);
        CSSStyleRule existingRule = findRuleBySelector(styleSheet, newRule);
        if (existingRule == null) {
            styleSheet.addRule(newRule);
        } else {
            CSSDeclaration newDeclaration = newRule.getDeclarationAtIndex(0);
            CSSDeclaration existingDeclaration = existingRule
                    .getDeclarationOfPropertyName(property);
            if (existingDeclaration == null) {
                existingRule.addDeclaration(newDeclaration);
            } else {
                // rule with given selector, property and value exists -> save
                // for undo
                existingDeclaration
                        .setExpression(newDeclaration.getExpression());
            }
        }
    }

    protected void removeCssProperty(CascadingStyleSheet styleSheet,
            String selector, String property) {
        // value not considered
        CSSStyleRule newRule = createStyleRule(selector, property, "inherit");
        CSSStyleRule existingRule = findRuleBySelector(styleSheet, newRule);
        if (existingRule != null) {
            removeProperty(existingRule, newRule);
            if (existingRule.getDeclarationCount() == 0) {
                styleSheet.removeRule(existingRule);
            }
        }
    }

    protected void writeStylesheet(CascadingStyleSheet styleSheet) {
        File styles = getStyleSheetFile();
        try {
            CSSWriter writer = new CSSWriter().setWriteHeaderText(true)
                    .setHeaderText(getHeaderText());
            writer.getSettings().setOptimizedOutput(false);
            writer.writeCSS(styleSheet, new FileWriter(styles));
        } catch (IOException e) {
            throw new ThemeEditorException("Cannot write " + styles.getPath(),
                    e);
        }
    }

    protected void sortStylesheet(CascadingStyleSheet styleSheet) {
        List<CSSStyleRule> sortedRules = styleSheet.getAllStyleRules().stream()
                .sorted(Comparator.comparing(
                        r -> r.getSelectorAtIndex(0).getAsCSSString()))
                .collect(Collectors.toList());
        for (CSSStyleRule rule : sortedRules) {
            List<CSSDeclaration> sortedDeclarations = rule.getAllDeclarations()
                    .stream()
                    .sorted(Comparator
                            .comparing(CSSDeclaration::getAsCSSString))
                    .collect(Collectors.toList());
            rule.removeAllDeclarations();
            sortedDeclarations.forEach(rule::addDeclaration);
        }
        sortedRules.forEach(styleSheet::removeRule);
        sortedRules.forEach(styleSheet::addRule);
    }

    protected CSSStyleRule createStyleRule(String selector, String property,
            String value) {
        return CSSReader
                .readFromString(selector + "{" + property + ": " + value + "}",
                        StandardCharsets.UTF_8, ECSSVersion.LATEST)
                .getStyleRuleAtIndex(0);
    }

    protected void removeProperty(CSSStyleRule existingRule,
            CSSStyleRule newRule) {
        CSSDeclaration newDeclaration = newRule.getDeclarationAtIndex(0);
        String property = newDeclaration.getProperty();
        CSSDeclaration declaration = existingRule
                .getDeclarationOfPropertyName(property);
        if (declaration != null) {
            existingRule.removeDeclaration(declaration);
        }
    }

    protected CSSStyleRule findRuleBySelector(CascadingStyleSheet styleSheet,
            CSSStyleRule rule) {
        return styleSheet.getAllStyleRules().stream().filter(
                r -> r.getAllSelectors().containsAll(rule.getAllSelectors()))
                .findFirst().orElse(null);
    }

    protected void insertImportIfNotExists() {
        File themes = new File(getFrontendFolder(), "themes");
        String themeName = getThemeName(themes);
        File theme = new File(themes, themeName);
        File themeStyles = new File(theme, "styles.css");

        CascadingStyleSheet styleSheet = CSSReader.readFromFile(themeStyles,
                StandardCharsets.UTF_8, ECSSVersion.LATEST);

        CSSImportRule expectedRule = new CSSImportRule(getCssFileName());
        if (!styleSheet.getAllImportRules().contains(expectedRule)) {
            FileWriter writer = null;
            try {
                List<String> lines = new ArrayList<>();
                lines.add("@import \"" + getCssFileName() + "\";");
                lines.addAll(IOUtils.readLines(new FileReader(themeStyles)));
                themeStyles.delete();
                themeStyles.createNewFile();
                writer = new FileWriter(themeStyles);
                IOUtils.writeLines(lines, System.lineSeparator(), writer);
            } catch (IOException e) {
                throw new ThemeEditorException(
                        "Cannot insert theme-editor.css @import", e);
            } finally {
                IOUtils.closeQuietly(writer);
            }
        }
    }

    protected String getThemeName(File themes) {
        String[] themeFolders = themes.list();
        if (themeFolders == null || themeFolders.length == 0) {
            throw new ThemeEditorException(
                    "No theme folder found in " + themes.getAbsolutePath());
        } else if (themeFolders.length > 1) {
            throw new ThemeEditorException("Multiple theme folders found in "
                    + themes.getAbsolutePath()
                    + ". I don't know which to update");
        }

        return themeFolders[0];
    }

}
