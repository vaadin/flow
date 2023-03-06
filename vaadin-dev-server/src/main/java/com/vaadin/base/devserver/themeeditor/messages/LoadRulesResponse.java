package com.vaadin.base.devserver.themeeditor.messages;

import java.util.List;
import java.util.Map;

public class LoadRulesResponse extends BaseResponse {
    public record CssRule(String selector, Map<String, String> properties) {
    }

    private List<CssRule> rules;

    public LoadRulesResponse(String requestId, List<CssRule> rules) {
        super(requestId, CODE_OK);
        this.rules = rules;
    }

    public List<CssRule> getRules() {
        return rules;
    }

    public void setRules(List<CssRule> rules) {
        this.rules = rules;
    }
}
