package com.vaadin.base.devserver;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ThemeModifier {

    private static final String THEME_EDITOR_CSS = "theme-editor.css";

    private final String HEADER_TEXT = "This file has been created by Vaadin, please be concerned that\n"
            + "manual changes may be overwritten while using ThemeEditor";

    private VaadinContext context;

    public ThemeModifier(VaadinContext context) {
        this.context = context;
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

        return themeEditorStyles;
    }

    /**
     * Sets CSS rule in theme-editor.css. If rule is already present -
     * overwrites it.
     */
    public void setCssRule(String selector, String property, String value) {
        File styles = getThemeEditorStyleSheet();
        CascadingStyleSheet styleSheet = CSSReader.readFromFile(styles,
                StandardCharsets.UTF_8, ECSSVersion.LATEST);

        CSSStyleRule rule = parseStyleRule(selector, property, value);
        removeRuleIfExists(styleSheet, rule);

        styleSheet.addRule(rule);

        try {
            new CSSWriter().setWriteHeaderText(true).setHeaderText(HEADER_TEXT)
                    .writeCSS(styleSheet, new FileWriter(styles));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write " + styles.getPath(),
                    e);
        }
    }

    private CSSStyleRule parseStyleRule(String selector, String property,
            String value) {
        return CSSReader
                .readFromString(selector + "{" + property + ": " + value + "}",
                        StandardCharsets.UTF_8, ECSSVersion.LATEST)
                .getStyleRuleAtIndex(0);
    }

    protected void removeRuleIfExists(CascadingStyleSheet styleSheet,
            CSSStyleRule rule) {
        String propertyName = rule.getDeclarationAtIndex(0).getProperty();
        styleSheet.getAllStyleRules().stream()
                .filter(r -> r.getAllSelectors()
                        .containsAll(rule.getAllSelectors()))
                .filter(r -> r
                        .getDeclarationOfPropertyName(propertyName) != null)
                .findFirst().ifPresent(styleSheet::removeRule);
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
        // int uiId = (int) data.getNumber("uiId");
        // int nodeId = (int) data.getNumber("nodeId");
        JsonArray rules = data.getArray("rules");
        for (int i = 0; i < rules.length(); ++i) {
            JsonObject rule = rules.getObject(i);
            setCssRule(rule.getString("selector"), rule.getString("property"),
                    rule.getString("value"));
        }
    }

}
