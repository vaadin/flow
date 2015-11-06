package com.vaadin.ui;

import com.vaadin.hummingbird.kernel.StateNode;

public class IdGenerator {

    public static class SequenceNumber {
        private int number = 0;
    }

    public static String generateId(UI ui) {
        StateNode uiNode = ui.getElement().getNode();
        SequenceNumber seqNum = uiNode.get(SequenceNumber.class,
                SequenceNumber.class);
        if (seqNum == null) {
            seqNum = new SequenceNumber();
            uiNode.put(SequenceNumber.class, seqNum);
        }

        return "gen-" + seqNum.number++;
    }

}
