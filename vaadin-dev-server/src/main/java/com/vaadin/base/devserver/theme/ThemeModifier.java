package com.vaadin.base.devserver.theme;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

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

    private File getFrontendFolder() {
        return new File(ApplicationConfiguration.get(context).getStringProperty(
                FrontendUtils.PROJECT_BASEDIR, null), "frontend");
    }

    protected File getThemeEditorStyleSheet() {
        File themes = new File(getFrontendFolder(), "themes");
        String themeName = getThemeName(themes);
        File theme = new File(themes, themeName);
        File styles = new File(theme, "styles.css");
        File themeEditorStyles = new File(theme, THEME_EDITOR_CSS);

        // TODO Add @import() if not present not messing users styles.css
        // CascadingStyleSheet styleSheet = CSSReader.readFromFile(styles,
        // StandardCharsets.UTF_8, ECSSVersion.LATEST);
        // if (!styleSheet.getAllImportRules().contains(themeEditorImportRule))
        // {
        // styleSheet.addImportRule(0, themeEditorImportRule);
        // try {
        // new CSSWriter().writeCSS(styleSheet, new FileWriter(styles));
        // } catch (IOException e) {
        // throw new IllegalStateException("Cannot write " + styles.getPath());
        // }
        // }

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

    public void setCssRule(String selector, String property, String value) {
        File styles = getThemeEditorStyleSheet();
        CascadingStyleSheet styleSheet = CSSReader.readFromFile(styles,
                StandardCharsets.UTF_8, ECSSVersion.LATEST);

        CSSStyleRule rule = new CSSStyleRule();
        rule.addSelector(new CSSSelector()
                .addMember(new CSSSelectorSimpleMember(selector)));
        rule.addDeclaration(new CSSDeclaration(property,
                CSSExpression.createSimple(value)));

        removeRuleIfExists(styleSheet, rule.getSelectorAtIndex(0), property);

        styleSheet.addRule(rule);

        try {
            new CSSWriter().setWriteHeaderText(true).setHeaderText(HEADER_TEXT)
                    .writeCSS(styleSheet, new FileWriter(styles));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write " + styles.getPath(),
                    e);
        }
    }

    protected void removeRuleIfExists(CascadingStyleSheet styleSheet,
            CSSSelector selector, String property) {
        styleSheet.getAllStyleRules().stream()
                .filter(r -> r.getAllSelectors().contains(selector))
                .filter(r -> r.getDeclarationOfPropertyName(property) != null)
                .findFirst().ifPresent(styleSheet::removeRule);
    }

    private String getThemeName(File themes) {
        String[] themeFolders = themes.list();
        if (themeFolders.length == 0) {
            throw new IllegalStateException(
                    "No theme folder found in " + themes.getAbsolutePath());
        } else if (themeFolders.length > 1) {
            throw new IllegalStateException("Multiple theme folders found in "
                    + themes.getAbsolutePath()
                    + ". I don't know which to update");
        }

        return themeFolders[0];
    }

}
