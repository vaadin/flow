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
import java.util.List;
import java.util.Optional;

public class RulesHandler implements MessageHandler {

    private final HasThemeModifier hasThemeModifier;

    public RulesHandler(HasThemeModifier hasThemeModifier) {
        this.hasThemeModifier = hasThemeModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        RulesRequest request = JsonUtils.readToObject(data, RulesRequest.class);
        List<CssRule> currentRules = new ArrayList<>();
        for (CssRule rule : request.getRules()) {
            List<CssRule> existingRules = hasThemeModifier.getThemeModifier()
                    .getCssRules(
                            selector -> selector.equals(rule.getSelector()));
            if (!existingRules.isEmpty()) {
                // rule exists, calculate undo rule
                currentRules.add(buildUndoRule(rule, existingRules.get(0)));
            } else {
                // rule does not exist, need empty rule to be removed
                currentRules.add(buildEmptyRule(rule));
            }
        }
        return new ExecuteAndUndo(() -> {
            hasThemeModifier.getThemeModifier()
                    .setThemeProperties(request.getRules());
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
        CssRule undoRule = new CssRule();
        undoRule.setSelector(newRule.getSelector());
        newRule.getProperties().keySet()
                .forEach(prop -> undoRule.getProperties().put(prop,
                        existingRule.getProperties().get(prop)));
        return undoRule;
    }

    private CssRule buildEmptyRule(CssRule rule) {
        CssRule emptyRule = new CssRule();
        emptyRule.setSelector(rule.getSelector());
        rule.getProperties().keySet()
                .forEach(prop -> emptyRule.getProperties().put(prop, null));
        return emptyRule;
    }

}