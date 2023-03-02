package com.vaadin.base.devserver.themeeditor.messages;

import java.util.List;

public record ClassNamesRequest(String requestId,int uiId,int nodeId,List<String>add,List<String>remove){

public static final String COMMAND_NAME="themeEditorClassNames";

}
