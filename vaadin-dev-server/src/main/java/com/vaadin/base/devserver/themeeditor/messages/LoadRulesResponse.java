package com.vaadin.base.devserver.themeeditor.messages;

import com.vaadin.base.devserver.themeeditor.utils.CssRule;

import java.util.List;

public class LoadRulesResponse extends BaseResponse {

    private List<CssRule> rules;

    public LoadRulesResponse(List<CssRule> rules) {
        this.rules = rules;
    }

    public List<CssRule> getRules() {
        return rules;
    }

    public void setRules(List<CssRule> rules) {
        this.rules = rules;
    }

}
