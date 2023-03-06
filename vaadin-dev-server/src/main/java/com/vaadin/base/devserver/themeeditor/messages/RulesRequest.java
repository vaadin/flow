package com.vaadin.base.devserver.themeeditor.messages;

import java.util.List;

public class RulesRequest extends BaseRequest {

    public record CssRuleProperty(String selector, String property,
            String value) {
    }

    public static final String COMMAND_NAME = "themeEditorRules";

    private List<CssRuleProperty> add;

    private List<CssRuleProperty> remove;

    public RulesRequest() {

    }

    public RulesRequest(String requestId, List<CssRuleProperty> add,
            List<CssRuleProperty> remove) {
        super(requestId);
        this.add = add;
        this.remove = remove;
    }

    public List<CssRuleProperty> getAdd() {
        return add;
    }

    public void setAdd(List<CssRuleProperty> add) {
        this.add = add;
    }

    public List<CssRuleProperty> getRemove() {
        return remove;
    }

    public void setRemove(List<CssRuleProperty> remove) {
        this.remove = remove;
    }
}
