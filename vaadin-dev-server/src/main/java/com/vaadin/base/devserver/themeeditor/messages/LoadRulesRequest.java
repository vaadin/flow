package com.vaadin.base.devserver.themeeditor.messages;

import java.util.List;

public class LoadRulesRequest extends BaseRequest {

    private List<String> selectors;

    public List<String> getSelectors() {
        return selectors;
    }

    public void setSelectors(List<String> selectors) {
        this.selectors = selectors;
    }
}
