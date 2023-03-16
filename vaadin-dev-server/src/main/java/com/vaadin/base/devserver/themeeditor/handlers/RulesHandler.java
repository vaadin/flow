package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.RulesRequest;
import com.vaadin.base.devserver.themeeditor.messages.RulesResponse;
import com.vaadin.base.devserver.themeeditor.utils.*;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.*;

public class RulesHandler implements MessageHandler {

    private final HasThemeModifier hasThemeModifier;

    private final HasSourceModifier hasSourceModifier;

    private class FinalsHolder {
        private String className;
    }

    public RulesHandler(HasThemeModifier hasThemeModifier,
            HasSourceModifier hasSourceModifier) {
        this.hasThemeModifier = hasThemeModifier;
        this.hasSourceModifier = hasSourceModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        RulesRequest request = JsonUtils.readToObject(data, RulesRequest.class);

        // needs to be final for ExecuteAndUndo lambdas
        final List<CssRule> rules = new ArrayList<>(request.getRules());

        FinalsHolder holder = new FinalsHolder();

        // in case of instance request - load or generate unique class name
        if (request.isInstanceRequest()) {
            boolean accessible = hasSourceModifier.getSourceModifier()
                    .isAccessible(request.getUiId(), request.getNodeId());
            if (accessible) {
                holder.className = hasSourceModifier.getSourceModifier()
                        .getUniqueClassName(request.getUiId(),
                                request.getNodeId(), true);
                rules.forEach(rule -> rule.setClassName(holder.className));
            } else {
                throw new ThemeEditorException(
                        "Cannot modify unique CSS rules on inaccessible component.");
            }
        }

        List<CssRule> currentRules = new ArrayList<>();
        for (CssRule rule : rules) {
            List<CssRule> existingRules = hasThemeModifier.getThemeModifier()
                    .getCssRules(rule.getSelector()::equals);
            if (!existingRules.isEmpty()) {
                // rule exists, calculate undo rule
                currentRules.add(buildUndoRule(rule, existingRules.get(0)));
            } else {
                // rule does not exist, need empty rule to be removed
                currentRules.add(buildRuleWithEmptyValues(rule));
            }
        }
        return new ExecuteAndUndo(() -> {
            hasThemeModifier.getThemeModifier().setThemeProperties(rules);
            return new RulesResponse(holder.className);
        }, Optional.of(() -> {
            hasThemeModifier.getThemeModifier()
                    .setThemeProperties(currentRules);
            return new RulesResponse(holder.className);
        }));
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.RULES;
    }

    private CssRule buildUndoRule(CssRule newRule, CssRule existingRule) {
        Map<String, String> properties = new HashMap<>();
        newRule.getProperties().forEach((k, v) -> properties.put(k,
                existingRule.getProperties().get(k)));
        CssRule undoRule = newRule.clone();
        undoRule.setProperties(properties);
        return undoRule;
    }

    private CssRule buildRuleWithEmptyValues(CssRule rule) {
        Map<String, String> properties = new HashMap<>();
        rule.getProperties().forEach((k, v) -> properties.put(k, ""));
        CssRule emptyRule = rule.clone();
        emptyRule.setProperties(properties);
        return emptyRule;
    }

}