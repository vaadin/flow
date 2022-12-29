package com.vaadin.flow.component;

import com.vaadin.flow.server.VaadinSession;

/**
 * A component reference from the browser.
 */
public class ComponentReference {

    private int nodeId;
    private String appId;
    private VaadinSession session;

    /**
     * Creates a new reference.
     *
     * @param nodeId
     *            the node id
     * @param appId
     *            the application id
     */
    public ComponentReference(VaadinSession session, int nodeId, String appId) {
        this.session = session;
        this.nodeId = nodeId;
        this.appId = appId;
    }

    /**
     * Gets the session.
     *
     * @return the session
     */
    public VaadinSession getSession() {
        return session;
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
