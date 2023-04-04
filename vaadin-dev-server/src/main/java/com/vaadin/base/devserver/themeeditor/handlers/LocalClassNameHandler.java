package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
import com.vaadin.base.devserver.themeeditor.messages.SetClassNameRequest;
import com.vaadin.base.devserver.themeeditor.utils.HasSourceModifier;
import com.vaadin.base.devserver.themeeditor.utils.HasThemeModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.Optional;

public class LocalClassNameHandler implements MessageHandler {

    private final HasSourceModifier hasSourceModifier;

    private final HasThemeModifier hasThemeModifier;

    public LocalClassNameHandler(HasSourceModifier hasSourceModifier,
            HasThemeModifier hasThemeModifier) {
        this.hasSourceModifier = hasSourceModifier;
        this.hasThemeModifier = hasThemeModifier;
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
        String tagName = hasSourceModifier.getSourceModifier().getTag(uiId,
                nodeId);
        return new ExecuteAndUndo(() -> {
            // set classname in Java files
            hasSourceModifier.getSourceModifier().setLocalClassName(uiId,
                    nodeId, request.getClassName());

            // update CSS if local classname already present
            if (currentLocalClassName != null) {
                hasThemeModifier.getThemeModifier().replaceClassName(tagName,
                        currentLocalClassName, request.getClassName());
            }

            return BaseResponse.ok();
        }, Optional.of(() -> {
            if (currentLocalClassName != null) {
                // set previous value and rollback theme change
                hasSourceModifier.getSourceModifier().setLocalClassName(uiId,
                        nodeId, currentLocalClassName);
                hasThemeModifier.getThemeModifier().replaceClassName(tagName,
                        request.getClassName(), currentLocalClassName);
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