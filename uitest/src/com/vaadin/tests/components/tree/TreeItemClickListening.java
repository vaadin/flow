package com.vaadin.tests.components.tree;

import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Tree;

public class TreeItemClickListening extends AbstractTestUIWithLog {

    private int clickCounter = 0;

    @Override
    protected void setup(VaadinRequest request) {

        Tree tree = new Tree();

        tree.addContainerProperty("caption", String.class, "");
        for (int i = 1; i <= 2; i++) {
            String item = "Node " + i;
            tree.addItem(item);
            tree.getContainerProperty(item, "caption").setValue("Caption " + i);
            tree.setChildrenAllowed(item, false);
        }
        tree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        tree.setItemCaptionPropertyId("caption");

        tree.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                clickCounter++;
                switch (event.getButton()) {
                case LEFT:
                    log.log("Left Click");
                    break;
                case RIGHT:
                    log.log("Right Click");
                    break;
                }
            }
        });

        addComponent(tree);
    }

    @Override
    protected String getTestDescription() {
        return "Item click event should be triggered from all mouse button clicks";
    }

    @Override
    protected Integer getTicketNumber() {
        return 6845;
    }
}
