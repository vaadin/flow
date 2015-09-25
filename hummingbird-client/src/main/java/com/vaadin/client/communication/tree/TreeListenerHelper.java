package com.vaadin.client.communication.tree;

import java.util.Objects;

import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.communication.tree.TreeNode.TreeNodeChangeListener;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;

public class TreeListenerHelper {

    public static HandlerRegistration addListener(TreeNode node, String path,
            TreeNodePropertyValueChangeListener listener) {
        int firstDot = path.indexOf('.');
        if (firstDot == -1) {
            // No hierarchy
            if (node.hasProperty(path)) {
                return node.getProperty(path)
                        .addPropertyChangeListener(listener);
            } else {
                return new HandlerRegistration() {
                    HandlerRegistration subRegistration;

                    {
                        subRegistration = node.addTreeNodeChangeListener(
                                new TreeNodeChangeListener() {
                            @Override
                            public void addProperty(String name,
                                    TreeNodeProperty property) {
                                if (path.equals(name)) {
                                    subRegistration.removeHandler();
                                    subRegistration = addListener(node, path,
                                            listener);
                                }
                            }

                            @Override
                            public void addArray(String name,
                                    EventArray array) {
                                // Don't care
                            }
                        });
                    }

                    @Override
                    public void removeHandler() {
                        subRegistration.removeHandler();
                    }
                };
            }
        } else {
            String start = path.substring(0, firstDot);
            String rest = path.substring(firstDot + 1);

            return new HandlerRegistration() {
                HandlerRegistration innerRegistration;
                HandlerRegistration outerRegistration = addListener(node, start,
                        new TreeNodePropertyValueChangeListener() {
                    @Override
                    public void changeValue(Object oldValue, Object newValue) {
                        if (innerRegistration != null) {
                            innerRegistration.removeHandler();
                            innerRegistration = null;
                        }

                        TreeNode oldNode = (TreeNode) oldValue;
                        TreeNode newNode = (TreeNode) newValue;

                        Object oldPropertyValue = findValue(oldNode, rest);
                        Object newPropertyValue = findValue(newNode, rest);

                        if (newNode != null) {
                            innerRegistration = addListener(newNode, rest,
                                    listener);
                        }

                        if (Objects.equals(oldPropertyValue,
                                newPropertyValue)) {
                            listener.changeValue(oldPropertyValue,
                                    newPropertyValue);
                        }
                    }

                    private Object findValue(TreeNode node, String path) {
                        String[] parts = path.split("\\.");
                        for (int i = 0; i < parts.length; i++) {
                            String part = parts[i];
                            if (node != null && node.hasProperty(part)) {
                                Object value = node.getProperty(part)
                                        .getValue();
                                if (i == parts.length - 1) {
                                    return value;
                                } else {
                                    node = (TreeNode) value;
                                }
                            } else {
                                return null;
                            }
                        }
                        throw new RuntimeException();
                    }
                });

                @Override
                public void removeHandler() {
                    if (innerRegistration != null) {
                        innerRegistration.removeHandler();
                    }
                    outerRegistration.removeHandler();
                }
            };
        }
    }
}
