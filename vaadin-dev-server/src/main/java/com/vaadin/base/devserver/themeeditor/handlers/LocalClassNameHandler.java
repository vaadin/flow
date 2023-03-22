package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
import com.vaadin.base.devserver.themeeditor.messages.SetClassNameRequest;
import com.vaadin.base.devserver.themeeditor.utils.HasSourceModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.Optional;

public class LocalClassNameHandler implements MessageHandler {

    private final HasSourceModifier hasSourceModifier;

    public LocalClassNameHandler(HasSourceModifier hasSourceModifier) {
        this.hasSourceModifier = hasSourceModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        SetClassNameRequest request = JsonUtils.readToObject(data,
                SetClassNameRequest.class);

        if (!request.isInstanceRequest()) {
            throw new ThemeEditorException(
                    "Cannot load metadata - uiId or nodeId are missing.");
        }

        int uiId = request.getUiId();
        int nodeId = request.getNodeId();
        String currentLocalClassName = hasSourceModifier.getSourceModifier()
                .getLocalClassName(uiId, nodeId);
        return new ExecuteAndUndo(() -> {
            hasSourceModifier.getSourceModifier().setLocalClassName(uiId,
                    nodeId, request.getClassName());
            return BaseResponse.ok();
        }, Optional.of(() -> {
            if (currentLocalClassName != null) {
                // set previous value
                hasSourceModifier.getSourceModifier().setLocalClassName(uiId,
                        nodeId, currentLocalClassName);
            } else {
                // remove current value
                hasSourceModifier.getSourceModifier().removeLocalClassName(uiId,
                        nodeId);
            }
            return BaseResponse.ok();
        }));
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.LOCAL_CLASS_NAME;
    }

}