package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
import com.vaadin.base.devserver.themeeditor.messages.ClassNamesRequest;
import com.vaadin.base.devserver.themeeditor.utils.HasSourceModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.Collection;
import java.util.Optional;

public class ClassNamesHandler implements MessageHandler {

    private final HasSourceModifier hasSourceModifier;

    public ClassNamesHandler(HasSourceModifier hasSourceModifier) {
        this.hasSourceModifier = hasSourceModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        ClassNamesRequest request = JsonUtils.readToObject(data,
                ClassNamesRequest.class);
        int uiId = request.getUiId();
        int nodeId = request.getNodeId();
        return new ExecuteAndUndo(() -> {
            if (isNotEmpty(request.getAdd())) {
                hasSourceModifier.getSourceModifier().setClassNames(uiId,
                        nodeId, request.getAdd());
            }
            if (isNotEmpty(request.getRemove())) {
                hasSourceModifier.getSourceModifier().removeClassNames(uiId,
                        nodeId, request.getRemove());
            }
            return BaseResponse.ok();
        }, Optional.of(() -> {
            if (isNotEmpty(request.getAdd())) {
                hasSourceModifier.getSourceModifier().removeClassNames(uiId,
                        nodeId, request.getAdd());
            }
            if (isNotEmpty(request.getRemove())) {
                hasSourceModifier.getSourceModifier().setClassNames(uiId,
                        nodeId, request.getRemove());
            }
            return BaseResponse.ok();
        }));
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.CLASS_NAMES;
    }

    private boolean isNotEmpty(Collection collection) {
        return collection != null && !collection.isEmpty();
    }
}