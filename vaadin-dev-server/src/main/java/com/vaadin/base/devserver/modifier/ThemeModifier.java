package com.vaadin.base.devserver.modifier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSExpression;
import com.helger.css.decl.CSSSelector;
import com.helger.css.decl.CSSSelectorSimpleMember;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class ThemeModifier {

    private VaadinContext context;

    public ThemeModifier(VaadinContext context) {
        this.context = context;
    }

    private File getFrontendFolder() {
        return new File(ApplicationConfiguration.get(context).getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                null));
    }

    public void updateCssProperty(String property, String value, String paletteMode) {
        File themes = new File(getFrontendFolder(), "themes");
        String themeName = getThemeName(themes);
        File theme = new File(themes, themeName);
        File styles = new File(theme, "styles.css");
        CascadingStyleSheet styleSheet = CSSReader.readFromFile(styles, StandardCharsets.UTF_8, ECSSVersion.LATEST);
        CSSStyleRule htmlHostRule = findHtmlHostrule(styleSheet, paletteMode);

        CSSDeclaration declaration = htmlHostRule.getAllDeclarations()
                .findFirst(decl -> decl.getProperty().equals(property));
        CSSExpression expression = value == null ? null : CSSExpression.createSimple(value);
        if (declaration == null) {
            if (expression != null) {
                declaration = new CSSDeclaration(property, expression);
                htmlHostRule.addDeclaration(declaration);
            }
        } else {
            if (expression == null) {
                htmlHostRule.removeDeclaration(declaration);
            } else {
                declaration.setExpression(expression);
            }
        }
        try {
            new CSSWriter().setWriteHeaderText(false).writeCSS(styleSheet, new FileWriter(styles));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String getThemeName(File themes) {
        String[] themeFolders = themes.list();
        if (themeFolders.length == 0) {
            throw new IllegalStateException("No theme folder found in " + themes.getAbsolutePath());
        } else if (themeFolders.length > 1) {
            throw new IllegalStateException(
                    "Multiple theme folders found in " + themes.getAbsolutePath() + ". I don't know which to update");
        }

        return themeFolders[0];
    }

    private static CSSStyleRule findHtmlHostrule(CascadingStyleSheet csss, String paletteMode) {
        ICommonsList<CSSStyleRule> allRules = csss.getAllStyleRules();
        CSSStyleRule rule = allRules.findFirst(r -> {
            if (r.getSelectorCount() != 2) {
                return false;
            }
            String selector1 = r.getSelectorAtIndex(0).getAsCSSString();
            String selector2 = r.getSelectorAtIndex(1).getAsCSSString();

            String expectedSelector1 = getExpectedSelector1(paletteMode);
            String expectedSelector2 = getExpectedSelector2(paletteMode);

            return (expectedSelector1.equals(selector1)
                    && expectedSelector2.equals(selector2));
        });

        if (rule == null) {
            rule = new CSSStyleRule();
            CSSSelector selector = new CSSSelector();
            CSSSelectorSimpleMember member = new CSSSelectorSimpleMember(
                    getExpectedSelector1(paletteMode) + ", " + getExpectedSelector2(paletteMode));
            selector.addMember(member);
            rule.addSelector(selector);
            csss.addRule(rule);
        }

        return rule;
    }

    private static String getExpectedSelector1(String paletteMode) {
        return getExpectedSelector(paletteMode, 0);
    }

    private static String getExpectedSelector2(String paletteMode) {
        return getExpectedSelector(paletteMode, 1);
    }

    private static String getExpectedSelector(String paletteMode, int idx) {
        if (paletteMode == null || paletteMode.equals("light")) {
            if (idx == 0) {
                return "html";
            } else {
                return ":host";
            }
        } else {
            String themeAttribute = "[theme~=\"" + paletteMode + "\"]";
            if (idx == 0) {
                return themeAttribute;
            } else {
                return ":host(" + themeAttribute + ")";
            }
        }

    }

    public void setDefaultThemePalette(String palette) {
        File indexHtml = new File(getFrontendFolder(), "index.html");
        Document doc;
        try {
            doc = Jsoup.parse(indexHtml, "utf-8");

            Elements htmlElement = doc.getElementsByTag("html");

            if (palette == null || palette.equals("light")) {
                htmlElement.removeAttr("theme");
            } else {
                htmlElement.attr("theme", palette);
            }
            FileUtils.write(indexHtml, doc.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
