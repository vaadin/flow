package com.vaadin.base.devserver.themeeditor.messages;

public class ComponentMetadataResponse extends BaseResponse {

    private boolean accessible;

    public ComponentMetadataResponse() {

    }

    public ComponentMetadataResponse(boolean accessible) {
        this.accessible = accessible;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public void setAccessible(boolean accessible) {
        this.accessible = accessible;
    }
}
