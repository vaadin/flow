package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Text;
import com.vaadin.client.Profiler;

public class TextElementListener {
    public static void bind(TreeNode node, Text textNode) {
        node.getProperty("content")
                .addPropertyChangeListener((oldValue, value) -> {
                    Profiler.enter("TextElementListener.changeValue");

                    textNode.setData(value == null ? "" : value.toString());

                    Profiler.leave("TextElementListener.changeValue");
                });

    }
}