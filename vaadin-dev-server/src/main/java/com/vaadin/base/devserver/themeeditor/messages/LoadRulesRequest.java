package com.vaadin.base.devserver.themeeditor.messages;

public class LoadRulesRequest extends BaseRequest {

    private String selectorFilter;

    public String getSelectorFilter() {
        return selectorFilter;
    }

    public void setSelectorFilter(String selectorFilter) {
        this.selectorFilter = selectorFilter;
    }
}
