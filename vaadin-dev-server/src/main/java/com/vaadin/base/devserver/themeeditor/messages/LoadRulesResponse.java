package com.vaadin.base.devserver.themeeditor.messages;

import com.vaadin.base.devserver.themeeditor.utils.CssRule;

import java.util.List;

public class LoadRulesResponse extends BaseResponse {

    private Boolean accessible;

    private List<CssRule> rules;

    public LoadRulesResponse(List<CssRule> rules, Boolean accessible) {
        this.rules = rules;
        this.accessible = accessible;
    }

    public List<CssRule> getRules() {
        return rules;
    }

    public void setRules(List<CssRule> rules) {
        this.rules = rules;
    }

    public Boolean isAccessible() {
        return accessible;
    }

    public void setAccessible(Boolean accessible) {
        this.accessible = accessible;
    }

}
