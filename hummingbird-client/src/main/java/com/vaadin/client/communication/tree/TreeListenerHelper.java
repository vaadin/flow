package com.vaadin.client.communication.tree;

import java.util.Objects;

import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.communication.tree.TreeNode.TreeNodeChangeListener;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;

public class TreeListenerHelper {

    public static TreeNodeProperty getProperty(TreeNode node, String path) {
        int firstDot = path.indexOf('.');
        if (firstDot == -1) {
            // No hierarchy
            if (!node.hasProperty(path)) {
                return null;
            } else {
                return node.getProperty(path);
            }
        } else {
            // Hierarchy
            String firstPart = path.substring(0, firstDot);
            String rest = path.substring(firstDot + 1);

            if (!node.hasProperty(firstPart)) {
                return null;
            }

            TreeNodeProperty firstProperty = node.getProperty(firstPart);
            if (firstProperty == null) {
                return null;
            }

            if (firstProperty.getValue() instanceof TreeNode) {
                return getProperty((TreeNode) firstProperty.getValue(), rest);
            } else {
                return null;
            }

        }
    }

    public static HandlerRegistration addListener(TreeNode node, String path,
            boolean createIfNecessary,
            TreeNodePropertyValueChangeListener listener) {
        TreeUpdater.debug("Binding listener to " + path);
        int firstDot = path.indexOf('.');
        if (firstDot == -1) {
            // No hierarchy
            if (createIfNecessary || node.hasProperty(path)) {
                TreeUpdater
                        .debug("Adding property change listener for " + path);
                return node.getProperty(path)
                        .addPropertyChangeListener(listener);
            } else {
                TreeUpdater.debug("Waiting for " + path + " to be created");
                return new HandlerRegistration() {
                    HandlerRegistration subRegistration;

                    {
                        subRegistration = node.addTreeNodeChangeListener(
                                new TreeNodeChangeListener() {
                            @Override
                            public void addProperty(String name,
                                    TreeNodeProperty property) {
                                if (path.equals(name)) {
                                    TreeUpdater.debug(path
                                            + " created, adding real listener");
                                    subRegistration.removeHandler();
                                    subRegistration = addListener(node, path,
                                            createIfNecessary, listener);
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
                        TreeUpdater.debug("Remove handler waiting for " + path
                                + " to be created");
                        subRegistration.removeHandler();
                    }
                };
            }
        } else {
            String start = path.substring(0, firstDot);
            String rest = path.substring(firstDot + 1);

            TreeUpdater.debug("Adding listener for " + start + " for " + path);

            return new HandlerRegistration() {
                HandlerRegistration innerRegistration;
                TreeNodePropertyValueChangeListener childListener = new TreeNodePropertyValueChangeListener() {

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

                        TreeUpdater.debug("Listener of " + start + " for "
                                + path + " triggered old value: " + oldValue
                                + ", new value: " + newValue
                                + ", old property value: " + oldPropertyValue
                                + ", new property value: " + newPropertyValue);

                        if (newNode != null) {
                            innerRegistration = addListener(newNode, rest,
                                    createIfNecessary, listener);
                        }

                        if (!Objects.equals(oldPropertyValue,
                                newPropertyValue)) {
                            TreeUpdater.debug("Notifying listener because "
                                    + start + " for " + path + " changed");
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
                };

                HandlerRegistration outerRegistration = addListener(node, start,
                        createIfNecessary, childListener);

                {
                    if (node.hasProperty(start))

                    {
                        Object value = node.getProperty(start).getValue();
                        if (value != null) {
                            childListener.changeValue(null, value);
                        }
                    }

                }

                @Override
                public void removeHandler() {
                    TreeUpdater.debug("No longer listening for " + start
                            + " for " + path);

                    if (innerRegistration != null) {
                        innerRegistration.removeHandler();
                    }
                    outerRegistration.removeHandler();
                }
            };
        }
    }
}
