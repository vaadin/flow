package com.vaadin.base.devserver.themeeditor.messages;

public record ComponentMetadataRequest(String requestId,int nodeId,int uiId){

public static final String COMMAND_NAME="themeEditorComponentMetadata";

}
