package com.vaadin.base.devserver.themeeditor.messages;

public class ComponentMetadataResponse extends BaseResponse {

    private Boolean accessible;

    private String className;

    private String suggestedClassName;

    public ComponentMetadataResponse(Boolean accessible, String className,
            String suggestedClassName) {
        this.accessible = accessible;
        this.className = className;
        this.suggestedClassName = suggestedClassName;
    }

    public Boolean isAccessible() {
        return accessible;
    }

    public void setAccessible(Boolean accessible) {
        this.accessible = accessible;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSuggestedClassName() {
        return suggestedClassName;
    }

    public void setSuggestedClassName(String suggestedClassName) {
        this.suggestedClassName = suggestedClassName;
    }
}
