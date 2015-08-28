package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Text;

public class TextElementListener implements NodeListener {

    private Text textNode;

    public TextElementListener(Text textNode) {
        this.textNode = textNode;
    }

    @Override
    public void putNode(PutNodeChange change) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void put(PutChange change) {
        String key = change.getKey();
        if (key.equals("content")) {
            textNode.setData(change.getValue().asString());
        }
    }

    @Override
    public void listInsertNode(ListInsertNodeChange change) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void listInsert(ListInsertChange change) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void listRemove(ListRemoveChange change) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void remove(RemoveChange change) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void putOverride(PutOverrideChange change) {
        throw new RuntimeException("Not yet implemented");
    }
}