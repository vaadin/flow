package com.vaadin.base.devserver;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSImportRule;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ThemeModifier {

    public enum State {
        ENABLED, DISABLED, MISSING_THEME
    }

    protected static final String THEME_EDITOR_ENABLED_PROPERTY = "vaadin.themeEditor.enable";

    private static final String THEME_EDITOR_CSS = "theme-editor.css";

    private static final String DEFAULT_THEME = "my-theme";

    private static final String HEADER_TEXT = "This file has been created by Vaadin, please be concerned that\n"
            + "manual changes may be overwritten while using ThemeEditor";

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

    protected State init() {
        // for development purposes only
        if (System.getProperty(THEME_EDITOR_ENABLED_PROPERTY) == null) {
            return State.DISABLED;
        }

        try {
            getThemeEditorStyleSheet();
        } catch (Exception ex) {
            return State.MISSING_THEME;
        }
        return State.ENABLED;
    }

    protected File getFrontendFolder() {
        return new File(ApplicationConfiguration.get(context).getStringProperty(
                FrontendUtils.PROJECT_BASEDIR, null), "frontend");
    }

    protected File getThemeEditorStyleSheet() {
        File themes = new File(getFrontendFolder(), "themes");
        String themeName = getThemeName(themes);
        File theme = new File(themes, themeName);
        File themeEditorStyles = new File(theme, THEME_EDITOR_CSS);

        if (!themeEditorStyles.exists()) {
            try {
                if (!themeEditorStyles.createNewFile()) {
                    throw new IllegalStateException(
                            "Cannot create " + themeEditorStyles.getPath());
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Cannot create " + themeEditorStyles.getPath(), e);
            }
        }

        if (!themeEditorStyles.canWrite()) {
            throw new IllegalStateException(
                    themeEditorStyles.getPath() + " is not writable.");
        }

        return themeEditorStyles;
    }

    public boolean createDefaultTheme() {
        File theme = new File(getFrontendFolder(), "themes/" + DEFAULT_THEME);
        if (!theme.exists()) {
            theme.mkdirs();
        }
        try {
            new File(theme, "styles.css").createNewFile();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Sets CSS rule in theme-editor.css. If rule is already present -
     * overwrites it.
     */
    public void setCssRule(String selector, String property, String value) {
        File styles = getThemeEditorStyleSheet();
        CascadingStyleSheet styleSheet = CSSReader.readFromFile(styles,
                StandardCharsets.UTF_8, ECSSVersion.LATEST);

        if (!importPresent) {
            insertImportIfNotExists();
            importPresent = true;
        }

        CSSStyleRule newRule = parseStyleRule(selector, property, value);
        findRuleBySelector(styleSheet, newRule).ifPresentOrElse(
                existingRule -> addOrUpdateProperty(existingRule, newRule),
                () -> styleSheet.addRule(newRule));

        sortStylesheet(styleSheet);
        writeStylesheet(styleSheet, styles);
    }

    /**
     * Removes CSS rule property and removes CSS rule itself if no more
     * properties are left.
     */
    public void removeCssRule(String selector, String property) {
        File styles = getThemeEditorStyleSheet();
        CascadingStyleSheet styleSheet = CSSReader.readFromFile(styles,
                StandardCharsets.UTF_8, ECSSVersion.LATEST);

        if (!importPresent) {
            insertImportIfNotExists();
            importPresent = true;
        }

        // value not considered
        CSSStyleRule newRule = parseStyleRule(selector, property, "inherit");
        Optional<CSSStyleRule> optRule = findRuleBySelector(styleSheet,
                newRule);
        if (optRule.isPresent()) {
            CSSStyleRule existingRule = optRule.get();
            removeProperty(existingRule, newRule);
            if (existingRule.getDeclarationCount() == 0) {
                styleSheet.removeRule(existingRule);
            }
        }

        writeStylesheet(styleSheet, styles);
    }

    protected void writeStylesheet(CascadingStyleSheet styleSheet,
            File styles) {
        try {
            CSSWriter writer = new CSSWriter().setWriteHeaderText(true)
                    .setHeaderText(HEADER_TEXT);
            writer.getSettings().setOptimizedOutput(false);
            writer.writeCSS(styleSheet, new FileWriter(styles));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write " + styles.getPath(),
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

    protected CSSStyleRule parseStyleRule(String selector, String property,
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

        CSSImportRule expectedRule = new CSSImportRule(THEME_EDITOR_CSS);
        if (!styleSheet.getAllImportRules().contains(expectedRule)) {
            FileWriter writer = null;
            try {
                List<String> lines = new ArrayList<>();
                lines.add("@import \"" + THEME_EDITOR_CSS + "\";");
                lines.addAll(IOUtils.readLines(new FileReader(themeStyles)));
                themeStyles.delete();
                themeStyles.createNewFile();
                writer = new FileWriter(themeStyles);
                IOUtils.writeLines(lines, System.lineSeparator(), writer);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Cannot insert theme-editor.css @import", e);
            } finally {
                IOUtils.closeQuietly(writer);
            }
        }
    }

    private String getThemeName(File themes) {
        String[] themeFolders = themes.list();
        if (themeFolders == null || themeFolders.length == 0) {
            throw new IllegalStateException(
                    "No theme folder found in " + themes.getAbsolutePath());
        } else if (themeFolders.length > 1) {
            throw new IllegalStateException("Multiple theme folders found in "
                    + themes.getAbsolutePath()
                    + ". I don't know which to update");
        }

        return themeFolders[0];
    }

    public void handleDebugMessageData(JsonObject data) {
        JsonArray rules = data.getArray("add");
        if (rules != null) {
            for (int i = 0; i < rules.length(); ++i) {
                JsonObject rule = rules.getObject(i);
                setCssRule(rule.getString("selector"),
                        rule.getString("property"), rule.getString("value"));
            }
        }
        rules = data.getArray("remove");
        if (rules != null) {
            for (int i = 0; i < rules.length(); ++i) {
                JsonObject rule = rules.getObject(i);
                removeCssRule(rule.getString("selector"),
                        rule.getString("property"));
            }
        }
    }

}
