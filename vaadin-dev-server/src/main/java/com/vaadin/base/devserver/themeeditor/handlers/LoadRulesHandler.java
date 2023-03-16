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

    private class FinalsHolder {
        private Predicate<String> filter;
        private Boolean accessible;
        private String className;
    }

    public LoadRulesHandler(HasThemeModifier hasThemeModifier,
            HasSourceModifier hasSourceModifier) {
        this.hasThemeModifier = hasThemeModifier;
        this.hasSourceModifier = hasSourceModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        LoadRulesRequest request = JsonUtils.readToObject(data,
                LoadRulesRequest.class);

        FinalsHolder holder = new FinalsHolder();

        // no filter by default
        holder.filter = selector -> true;

        // add selector filter
        if (request.getSelectorFilter() != null) {
            holder.filter = holder.filter.and(selector -> selector
                    .startsWith(request.getSelectorFilter()));
        }

        // add classname filter
        if (request.isInstanceRequest()) {
            holder.accessible = hasSourceModifier.getSourceModifier()
                    .isAccessible(request.getUiId(), request.getNodeId());
            if (holder.accessible) {
                holder.className = hasSourceModifier.getSourceModifier()
                        .getUniqueClassName(request.getUiId(),
                                request.getNodeId(), false);
                if (holder.className != null) {
                    holder.filter = holder.filter.and(selector -> selector
                            .contains("." + holder.className));
                } else {
                    // user requests rules for instance but there are none
                    holder.filter = selector -> false;
                }
            }
        }

        return new ExecuteAndUndo(() -> {
            List<CssRule> rules = hasThemeModifier.getThemeModifier()
                    .getCssRules(holder.filter);
            return new LoadRulesResponse(rules, holder.accessible,
                    holder.className);
        }, Optional.empty());
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.LOAD_RULES;
    }
}
