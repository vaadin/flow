package com.vaadin.client.communication.tree;

import java.util.Objects;

import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.Profiler;
import com.vaadin.client.communication.tree.TreeNode.TreeNodeChangeListener;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;

public class TreeListenerHelper {

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
                                Profiler.enter(
                                        "TreeListenerHelper.addProperty");

                                if (path.equals(name)) {
                                    TreeUpdater.debug(path
                                            + " created, adding real listener");
                                    subRegistration.removeHandler();
                                    subRegistration = addListener(node, path,
                                            createIfNecessary, listener);
                                }

                                Profiler.leave(
                                        "TreeListenerHelper.addProperty");
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
                        Profiler.enter(
                                "TreeListenerHelper.childListener.changeValue");

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

                        Profiler.leave(
                                "TreeListenerHelper.childListener.changeValue");
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
