package com.vaadin.base.devserver.themeeditor.messages;

public class LoadRulesRequest extends BaseRequest {
    public static final String COMMAND_NAME = "themeEditorLoadRules";

    private String selectorFilter;

    public String getSelectorFilter() {
        return selectorFilter;
    }

    public void setSelectorFilter(String selectorFilter) {
        this.selectorFilter = selectorFilter;
    }
}
