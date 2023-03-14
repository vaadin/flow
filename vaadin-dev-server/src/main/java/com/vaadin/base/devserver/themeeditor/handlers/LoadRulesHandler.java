package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.LoadRulesRequest;
import com.vaadin.base.devserver.themeeditor.messages.LoadRulesResponse;
import com.vaadin.base.devserver.themeeditor.utils.CssRule;
import com.vaadin.base.devserver.themeeditor.utils.HasSourceModifier;
import com.vaadin.base.devserver.themeeditor.utils.HasThemeModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class LoadRulesHandler implements MessageHandler {

    private final HasThemeModifier hasThemeModifier;

    private final HasSourceModifier hasSourceModifier;

    public LoadRulesHandler(HasThemeModifier hasThemeModifier,
            HasSourceModifier hasSourceModifier) {
        this.hasThemeModifier = hasThemeModifier;
        this.hasSourceModifier = hasSourceModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        LoadRulesRequest request = JsonUtils.readToObject(data,
                LoadRulesRequest.class);

        // no filter by default
        Predicate<String> filter = selector -> true;

        // in case of instance request - load or generate unique class name
        if (request.isInstanceRequest()) {
            String uniqueClassName = hasSourceModifier.getSourceModifier()
                    .getUniqueClassName(request.getUiId(), request.getNodeId());
            filter = filter
                    .and(selector -> selector.contains("." + uniqueClassName));
        }

        if (request.getSelectorFilter() != null) {
            filter = filter.and(selector -> selector
                    .startsWith(request.getSelectorFilter()));
        }

        final Predicate<String> filterFinal = filter;
        return new ExecuteAndUndo(() -> {
            List<CssRule> rules = hasThemeModifier.getThemeModifier()
                    .getCssRules(filterFinal);
            return new LoadRulesResponse(rules);
        }, Optional.empty());
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.LOAD_RULES;
    }
}
