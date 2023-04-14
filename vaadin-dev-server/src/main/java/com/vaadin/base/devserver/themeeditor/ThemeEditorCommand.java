package com.vaadin.base.devserver.themeeditor;

public interface ThemeEditorCommand {

    String CODE_OK = "ok";

    String CODE_ERROR = "error";

    String STATE = "themeEditorState";

    String RESPONSE = "themeEditorResponse";

    String COMPONENT_METADATA = "themeEditorComponentMetadata";

    String LOCAL_CLASS_NAME = "themeEditorLocalClassName";

    String HISTORY = "themeEditorHistory";

    String RULES = "themeEditorRules";

    String LOAD_RULES = "themeEditorLoadRules";

    String LOAD_PREVIEW = "themeEditorLoadPreview";

    String OPEN_CSS = "themeEditorOpenCss";

    String MARK_AS_USED = "themeEditorMarkAsUsed";

}
