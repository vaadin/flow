package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.LoadRulesRequest;
import com.vaadin.base.devserver.themeeditor.messages.LoadRulesResponse;
import com.vaadin.base.devserver.themeeditor.utils.CssRule;
import com.vaadin.base.devserver.themeeditor.utils.HasThemeModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.List;
import java.util.Optional;

public class LoadRulesHandler implements MessageHandler {

    private final HasThemeModifier hasThemeModifier;

    public LoadRulesHandler(HasThemeModifier hasThemeModifier) {
        this.hasThemeModifier = hasThemeModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        LoadRulesRequest request = JsonUtils.readToObject(data,
                LoadRulesRequest.class);
        List<CssRule> rules = hasThemeModifier.getThemeModifier()
                .getCssRules(request.getSelectors());
        return new ExecuteAndUndo(() -> new LoadRulesResponse(rules),
                Optional.empty());
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.LOAD_RULES;
    }
}
