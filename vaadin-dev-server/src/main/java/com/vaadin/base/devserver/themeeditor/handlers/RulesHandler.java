package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
import com.vaadin.base.devserver.themeeditor.messages.RulesRequest;
import com.vaadin.base.devserver.themeeditor.utils.CssRule;
import com.vaadin.base.devserver.themeeditor.utils.HasThemeModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RulesHandler implements MessageHandler {

    private final HasThemeModifier hasThemeModifier;

    public RulesHandler(HasThemeModifier hasThemeModifier) {
        this.hasThemeModifier = hasThemeModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        RulesRequest request = JsonUtils.readToObject(data, RulesRequest.class);

        // needs to be final for ExecuteAndUndo lambdas
        final List<CssRule> rules = new ArrayList<>(request.getRules());

        List<String> selectors = request.getRules().stream()
                .map(CssRule::getSelector).collect(Collectors.toList());

        List<CssRule> currentRules = new ArrayList<>();
        for (CssRule rule : rules) {
            List<CssRule> existingRules = hasThemeModifier.getThemeModifier()
                    .getCssRules(selectors);
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
            return BaseResponse.ok();
        }, Optional.of(() -> {
            hasThemeModifier.getThemeModifier()
                    .setThemeProperties(currentRules);
            return BaseResponse.ok();
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