package com.vaadin.base.devserver.themeeditor;

import com.vaadin.flow.server.VaadinContext;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for ThemeEditor debug window communication messages. Responsible for
 * preparing data for {@link ThemeModifier} and {@link JavaSourceModifier}.
 */
public class ThemeEditorMessageHandler {

    private final JavaSourceModifier sourceModifier;

    private final ThemeModifier themeModifier;

    public ThemeEditorMessageHandler(VaadinContext context) {
        sourceModifier = new JavaSourceModifier(context);
        themeModifier = new ThemeModifier(context);
    }

    public boolean isEnabled() {
        return themeModifier.isEnabled() && sourceModifier.isEnabled();
    }

    public String getState() {
        return themeModifier.getState().name().toLowerCase();
    }

    /**
     * Handles debug message command and performs given action.
     *
     * @param command
     *            Command name
     * @param data
     *            Command data
     * @return true if message has been handled, false otherwise
     */
    public boolean handleDebugMessageData(String command, JsonObject data) {
        try {
            switch (command) {
            case "themeEditorRules":
                List<ThemeModifier.CssRuleProperty> rulesToBeAdded = toCssRulePropertiesList(
                        data.getArray("add"));
                List<ThemeModifier.CssRuleProperty> rulesToBeRemoved = toCssRulePropertiesList(
                        data.getArray("remove"));
                themeModifier.setThemeProperties(rulesToBeAdded);
                themeModifier.removeThemeProperties(rulesToBeRemoved);
                return true;
            case "themeEditorCreateDefaultTheme":
                themeModifier.createDefaultTheme();
                return true;
            case "themeEditorClassName":
                int uiId = (int) data.getNumber("uiId");
                int nodeId = (int) data.getNumber("nodeId");
                List<String> classNamesToBeAdded = toClassNameList(
                        data.get("add"));
                List<String> classNamesToBeRemoved = toClassNameList(
                        data.get("remove"));
                sourceModifier.setClassNames(uiId, nodeId, classNamesToBeAdded);
                sourceModifier.removeClassNames(uiId, nodeId,
                        classNamesToBeRemoved);
                return true;
            default:
                return false;
            }
        } catch (ModifierException ex) {
            getLogger().error(ex.getMessage(), ex.getCause());
        }
        return true;
    }

    protected List<String> toClassNameList(JsonArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                list.add(array.getString(i));
            }
        }
        return list;
    }

    protected List<ThemeModifier.CssRuleProperty> toCssRulePropertiesList(
            JsonArray array) {
        List<ThemeModifier.CssRuleProperty> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                JsonObject rule = array.getObject(i);
                list.add(new ThemeModifier.CssRuleProperty(
                        rule.getString("selector"), rule.getString("property"),
                        rule.getString("value")));
            }
        }
        return list;
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(ThemeEditorMessageHandler.class.getName());
    }

}
