package com.vaadin.hummingbird.kernel;

import java.util.List;

public class ModelPath {

    private final String fullPath;
    private String[] path;

    public ModelPath(String fullPath, List<String> path) {
        this(fullPath, path.toArray(new String[path.size()]));
    }

    public ModelPath(String propertyName) {
        this(propertyName, new String[] { propertyName });
    }

    private ModelPath(String fullPath, String[] path) {
        this.fullPath = fullPath;
        this.path = path;
    }

    public String getNodeProperty() {
        if (path.length == 0) {
            return null;
        }
        return path[path.length - 1];
    }

    public StateNode getNode(StateNode node) {
        for (int i = 0; i < path.length - 1; i++) {
            String segment = path[i];
            if ("..".equals(segment)) {
                node = node.getParent();
            } else {
                node = node.get(segment, StateNode.class);
            }
        }
        return node;
    }

    public String getFullPath() {
        return fullPath;
    }

}
