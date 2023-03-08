package com.vaadin.base.devserver.themeeditor.messages;

import java.io.Serializable;

public class BaseRequest implements Serializable {

    private String requestId;

    private int nodeId;

    private int uiId;

    public BaseRequest() {
    }

    public BaseRequest(String requestId) {
        this.requestId = requestId;
    }

    public BaseRequest(String requestId, int nodeId, int uiId) {
        this.requestId = requestId;
        this.nodeId = nodeId;
        this.uiId = uiId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getUiId() {
        return uiId;
    }

    public void setUiId(int uiId) {
        this.uiId = uiId;
    }
}
