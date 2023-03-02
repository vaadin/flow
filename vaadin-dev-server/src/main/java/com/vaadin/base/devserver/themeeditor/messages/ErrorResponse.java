package com.vaadin.base.devserver.themeeditor.messages;

public record ErrorResponse(String requestId,String message){

public static final String COMMAND_NAME="themeEditorError";

}
