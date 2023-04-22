package com.vaadin.base.devserver.themeeditor.messages;

import com.vaadin.base.devserver.themeeditor.utils.CssRule;

import java.util.List;

public class RulesRequest extends BaseRequest {

    private List<CssRule> rules;

    public RulesRequest() {

    }

    public List<CssRule> getRules() {
        return rules;
    }

    public void setRules(List<CssRule> rules) {
        this.rules = rules;
    }

}
