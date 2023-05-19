package com.vaadin.base.devserver.themeeditor.messages;

public class LoadPreviewResponse extends BaseResponse {
    private String css;

    public LoadPreviewResponse(String css) {
        this.css = css;
    }

    public String getCss() {
        return css;
    }

    public void setCss(String css) {
        this.css = css;
    }
}
