package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.JavaSourceModifier;
import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.ComponentMetadataRequest;
import com.vaadin.base.devserver.themeeditor.messages.ComponentMetadataResponse;
import com.vaadin.base.devserver.themeeditor.utils.HasSourceModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.Optional;

public class ComponentMetadataHandler implements MessageHandler {

    private final HasSourceModifier hasSourceModifier;

    public ComponentMetadataHandler(HasSourceModifier hasSourceModifier) {
        this.hasSourceModifier = hasSourceModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        return new ExecuteAndUndo(() -> {
            ComponentMetadataRequest request = JsonUtils.readToObject(data,
                    ComponentMetadataRequest.class);
            JavaSourceModifier.ComponentMetadata metadata = hasSourceModifier
                    .getSourceModifier()
                    .getMetadata(request.getUiId(), request.getNodeId());
            return new ComponentMetadataResponse(metadata.isAccessible());
        }, Optional.empty());
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.COMPONENT_METADATA;
    }

}