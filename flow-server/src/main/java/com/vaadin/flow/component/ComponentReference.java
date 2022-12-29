package com.vaadin.flow.component;

/**
 * A component reference from the browser.
 */
public class ComponentReference {

    private int nodeId;
    private String appId;

    /**
     * Creates a new reference.
     *
     * @param nodeId
     *            the node id
     * @param appId
     *            the application id
     */
    public ComponentReference(int nodeId, String appId) {
        this.nodeId = nodeId;
        this.appId = appId;
    }

    /**
     * Gets the node id.
     *
     * @return the node id
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Gets the application id.
     *
     * @return the application id
     */
    public String getAppId() {
        return appId;
    }

}
