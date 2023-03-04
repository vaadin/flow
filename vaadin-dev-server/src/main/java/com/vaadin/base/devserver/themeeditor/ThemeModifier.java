package com.vaadin.base.devserver.themeeditor;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSImportRule;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import com.vaadin.base.devserver.themeeditor.messages.LoadRulesResponse;
import com.vaadin.base.devserver.themeeditor.messages.RulesRequest;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * Performs update of CSS file setting (adding or updating) given lists of
     * {@link RulesRequest.CssRuleProperty}.
     *
     * @param properties
     *            list of {@link RulesRequest.CssRuleProperty} to be added or
     *            updated
     */
    public void setThemeProperties(
            List<RulesRequest.CssRuleProperty> properties) {
        assert properties != null;
        CascadingStyleSheet styleSheet = getCascadingStyleSheet();
        properties.forEach(cssProp -> setCssProperty(styleSheet, cssProp));
        sortStylesheet(styleSheet);
        writeStylesheet(styleSheet);
    }

    /**
     * Performs update of CSS file setting and removing given lists of
     * {@link RulesRequest.CssRuleProperty}.
     *
     * @param properties
     *            list of {@link RulesRequest.CssRuleProperty} to be added or
     *            updated
     */
    public void removeThemeProperties(
            List<RulesRequest.CssRuleProperty> properties) {
        assert properties != null;
        CascadingStyleSheet styleSheet = getCascadingStyleSheet();
        properties.forEach(cssProp -> removeCssProperty(styleSheet, cssProp));
        sortStylesheet(styleSheet);
        writeStylesheet(styleSheet);
    }

    /**
     * Returns the full content of the theme editor CSS file.
     * @return CSS string
     */
    public String getCss() {
        File styles = getStyleSheetFile();
        try {
            return Files.readString(Path.of(styles.getAbsolutePath()));
        } catch (IOException e) {
            throw new ThemeEditorException("Could not read stylesheet from " + styles.getAbsolutePath());
        }
    }

    public List<LoadRulesResponse.CssRule> getCssRules(String selectorFilter) {
        CascadingStyleSheet styleSheet = getCascadingStyleSheet();
        CSSWriterSettings cssWriterSettings = new CSSWriterSettings();

        return styleSheet.getAllStyleRules().stream().filter(rule -> {
            String cssSelector = rule.getSelectorCount() > 0 ? rule.getSelectorAtIndex(0).getAsCSSString() : null;
            return cssSelector != null && cssSelector.startsWith(selectorFilter);
        }).map(rule -> {
            String selector = rule.getSelectorsAsCSSString(cssWriterSettings, 0);
            Map<String, String> properties = new HashMap<>();
            rule.getAllDeclarations().forEach(cssDeclaration -> {
                properties.put(cssDeclaration.getProperty(), cssDeclaration.getExpressionAsCSSString());
            });
            return new LoadRulesResponse.CssRule(selector, properties);
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
            RulesRequest.CssRuleProperty css) {
        CSSStyleRule newRule = createStyleRule(css.selector(), css.property(),
                css.value());
        findRuleBySelector(styleSheet, newRule).ifPresentOrElse(
                existingRule -> addOrUpdateProperty(existingRule, newRule),
                () -> styleSheet.addRule(newRule));
    }

    protected void removeCssProperty(CascadingStyleSheet styleSheet,
            RulesRequest.CssRuleProperty css) {
        // value not considered
        CSSStyleRule newRule = createStyleRule(css.selector(), css.property(),
                "inherit");
        Optional<CSSStyleRule> optRule = findRuleBySelector(styleSheet,
                newRule);
        if (optRule.isPresent()) {
            CSSStyleRule existingRule = optRule.get();
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

    protected void addOrUpdateProperty(CSSStyleRule existingRule,
            CSSStyleRule newRule) {
        CSSDeclaration newDeclaration = newRule.getDeclarationAtIndex(0);
        String property = newDeclaration.getProperty();
        CSSDeclaration declaration = existingRule
                .getDeclarationOfPropertyName(property);
        if (declaration == null) {
            existingRule.addDeclaration(newDeclaration);
        } else {
            declaration.setExpression(newDeclaration.getExpression());
        }
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

    protected Optional<CSSStyleRule> findRuleBySelector(
            CascadingStyleSheet styleSheet, CSSStyleRule rule) {
        return styleSheet.getAllStyleRules().stream().filter(
                r -> r.getAllSelectors().containsAll(rule.getAllSelectors()))
                .findFirst();
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
