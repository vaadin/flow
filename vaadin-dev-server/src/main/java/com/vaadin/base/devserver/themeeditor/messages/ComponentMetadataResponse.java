package com.vaadin.base.devserver.themeeditor.messages;

public class ComponentMetadataResponse extends BaseResponse {

    private boolean accessible;

    public ComponentMetadataResponse() {

    }

    public ComponentMetadataResponse(String requestId, boolean accessible) {
        super(requestId, CODE_OK);
        this.accessible = accessible;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public void setAccessible(boolean accessible) {
        this.accessible = accessible;
    }
}
