package com.vaadin.base.devserver.themeeditor.messages;

import java.util.List;

public record RulesRequest(String requestId,List<CssRuleProperty>add,List<CssRuleProperty>remove){

public static final String COMMAND_NAME="themeEditorRules";

public record CssRuleProperty(String selector,String property,String value){}

}
