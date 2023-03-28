package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.OpenInCurrentIde;
import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
import com.vaadin.base.devserver.themeeditor.messages.OpenCssRequest;
import com.vaadin.base.devserver.themeeditor.utils.HasThemeModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.io.File;
import java.util.Optional;

public class OpenCssHandler implements MessageHandler {

    private HasThemeModifier hasThemeModifier;

    public OpenCssHandler(HasThemeModifier hasThemeModifier) {
        this.hasThemeModifier = hasThemeModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        OpenCssRequest request = JsonUtils.readToObject(data,
                OpenCssRequest.class);
        String selector = request.getSelector();
        return new ExecuteAndUndo(() -> {
            File stylesheet = hasThemeModifier.getThemeModifier()
                    .getStyleSheetFile();
            int line = hasThemeModifier.getThemeModifier()
                    .getRuleLocationLine(selector);

            // rule not found, create empty for given selector
            if (line == -1) {
                hasThemeModifier.getThemeModifier()
                        .createEmptyStyleRule(selector);

                // locate new empty rule
                line = hasThemeModifier.getThemeModifier()
                        .getRuleLocationLine(selector);
                if (line == -1) {
                    throw new ThemeEditorException(
                            "Cannot create empty rule for " + selector);
                }
            }

            // open in IDE
            if (!OpenInCurrentIde.openFile(stylesheet, line)) {
                throw new ThemeEditorException("Cannot open "
                        + stylesheet.getAbsolutePath() + ":" + line);
            }
            return BaseResponse.ok();
        }, Optional.empty());
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.OPEN_CSS;
    }
}
