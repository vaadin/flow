package com.vaadin.base.devserver.themeeditor.messages;

import java.io.Serializable;

public class BaseRequest implements Serializable {

    private String requestId;

    private Integer nodeId;

    private Integer uiId;

    public BaseRequest() {
    }

    public BaseRequest(String requestId) {
        this.requestId = requestId;
    }

    public BaseRequest(String requestId, Integer nodeId, Integer uiId) {
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

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getUiId() {
        return uiId;
    }

    public void setUiId(Integer uiId) {
        this.uiId = uiId;
    }

    public boolean isInstanceRequest() {
        return getUiId() != null && getNodeId() != null;
    }
}
