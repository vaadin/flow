package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.BaseRequest;
import com.vaadin.base.devserver.themeeditor.messages.ComponentMetadataResponse;
import com.vaadin.base.devserver.themeeditor.utils.HasSourceModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.Optional;

public class ComponentMetadataHandler implements MessageHandler {

    private final HasSourceModifier hasSourceModifier;

    private static class FinalsHolder {
        private Boolean accessible;
        private String className;
        private String suggestedClassName;
    }

    public ComponentMetadataHandler(HasSourceModifier hasSourceModifier) {
        this.hasSourceModifier = hasSourceModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        BaseRequest request = JsonUtils.readToObject(data, BaseRequest.class);

        if (!request.isInstanceRequest()) {
            throw new ThemeEditorException(
                    "Cannot load metadata - uiId or nodeId are missing.");
        }

        FinalsHolder holder = new FinalsHolder();
        holder.accessible = hasSourceModifier.getSourceModifier()
                .isAccessible(request.getUiId(), request.getNodeId());
        if (holder.accessible) {
            holder.className = hasSourceModifier.getSourceModifier()
                    .getLocalClassName(request.getUiId(), request.getNodeId());
        }

        if (holder.accessible && holder.className == null) {
            holder.suggestedClassName = hasSourceModifier.getSourceModifier()
                    .getSuggestedClassName(request.getUiId(),
                            request.getNodeId());
            if (holder.suggestedClassName == null) {
                throw new ThemeEditorException(
                        "Cannot suggest classname for given component.");
            }
        }

        return new ExecuteAndUndo(
                () -> new ComponentMetadataResponse(holder.accessible,
                        holder.className, holder.suggestedClassName),
                Optional.empty());
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.COMPONENT_METADATA;
    }
}
