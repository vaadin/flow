package dev.hilla.push.messages.fromclient;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class PushConnectMessage extends AbstractMessage {

    private String endpointName, methodName;
    private ObjectNode params;

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public ObjectNode getParams() {
        return params;
    }

    public void setParams(ObjectNode params) {
        this.params = params;
    }

}
