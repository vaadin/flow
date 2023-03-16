package com.vaadin.base.devserver.themeeditor.messages;

import com.vaadin.base.devserver.themeeditor.utils.CssRule;

import java.util.List;

public class LoadRulesResponse extends BaseResponse {

    private Boolean accessible;

    private String className;

    private List<CssRule> rules;

    public LoadRulesResponse(List<CssRule> rules, Boolean accessible,
            String className) {
        this.rules = rules;
        this.accessible = accessible;
        this.className = className;
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
